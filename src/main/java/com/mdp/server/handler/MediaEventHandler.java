package com.mdp.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.AiServerClient;
import com.mdp.server.client.DbServerClient; // 💡 DB 전송을 위해 추가
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DeviceControlService;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class MediaEventHandler {

    private final MediaServerClient mediaServerClient;
    private final AiServerClient aiServerClient;
    private final DbServerClient dbServerClient; // 💡 추가됨
    private final DeviceControlService controlService;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final Executor mqttEventExecutor;

    public MediaEventHandler(
            MediaServerClient mediaServerClient,
            AiServerClient aiServerClient,
            DbServerClient dbServerClient, // 💡 생성자 주입 추가
            DeviceControlService controlService,
            WebSocketHandler webSocketHandler,
            ObjectMapper objectMapper,
            @Qualifier("mqttEventExecutor") Executor mqttEventExecutor
    ) {
        this.mediaServerClient = mediaServerClient;
        this.aiServerClient = aiServerClient;
        this.dbServerClient = dbServerClient;
        this.controlService = controlService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
        this.mqttEventExecutor = mqttEventExecutor;
    }

    public void handle(String topic, byte[] payload) {
        mqttEventExecutor.execute(() -> process(topic, payload));
    }

    private void process(String topic, byte[] payload) {
        String[] topicParts = topic.split("/");
        if (topicParts.length < 6) return;

        String teamId = topicParts[2];      // 조 이름 (예: "cty", "house")
        String deviceName = topicParts[3];  // 기기이름
        String fileName = topicParts[5];    // 파일명

        String[] fileParts = fileName.split("-");
        if (fileParts.length < 3) return;

        String analysisType = fileParts[1]; // "fire_image_detection" 등
        double confidence = 0.0;
        try {
            confidence = Double.parseDouble(fileParts[2]);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 확률값 추출 실패: " + fileParts[2]);
            return;
        }

        try {
            // 1. 미디어 서버 파일 업로드
            String uploadResponseJson = mediaServerClient.uploadImage(teamId, fileName, payload);
            Map<String, String> responseMap = objectMapper.readValue(uploadResponseJson, new TypeReference<Map<String, String>>() {});
            String fullImageUrl = "http://192.168.0.20:8090" + responseMap.get("fileUrl");

            boolean isDangerDetected = false;

            // 2. 확률 기반 분기 처리
            if (confidence >= 0.9) {
                System.out.println("[ALERT] 고확률 상황 (" + confidence + ") 즉시 상황 전개");
                sendAlertToApp(teamId, analysisType, fullImageUrl, "실시간 위험 감지");
                isDangerDetected = true;

            } else if (confidence >= 0.5) {
                System.out.println("[ANALYSIS] 모호한 확률 (" + confidence + ") -> AI 서버 재검증 요청");
                String aiResult = aiServerClient.requestInference(teamId, analysisType, fullImageUrl, System.currentTimeMillis());

                if (aiResult != null && aiResult.contains("detected=true")) {
                    System.out.println("[ALERT] AI가 위험 상황 최종 확정함");
                    sendAlertToApp(teamId, analysisType, fullImageUrl, "AI 분석 결과, 위험 상황 확정");
                    isDangerDetected = true;
                } else {
                    System.out.println("[SAFE] AI 검증 결과: 정상(오탐)으로 판단됨");
                }
            }

            // 4. [비즈니스 로직] 제어 및 추가 연동
            if (isDangerDetected) {

                // Case A: 소방차/구급차 등 긴급 차량 감지 시
                if (analysisType.contains("emergency")) {
                    controlService.sendTrafficLightCommand();
                }

                // Case B: 화재 이미지 감지 시 (공통 제어)
                if (analysisType.contains("fire")) {
                    controlService.sendFireAlertLeds();
                }

                // 💡 [새로 추가된 로직] Case C: 시골팀(cty) 화재 감지 시 JSON 생성 및 동시 전송
                if ("cty".equals(teamId) && analysisType.contains("fire")) {
                    sendCtyFireData(fullImageUrl); // 코드가 길어지지 않게 별도 메서드로 분리
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 미디어 파일 처리 오류");
            e.printStackTrace();
        }
    }
    // 💡 시골팀(cty) 화재 시 DB 및 웹소켓 전송을 담당하는 새 메서드
    private void sendCtyFireData(String fullImageUrl) {

        // 1. 개발자님이 만든 DataDto 객체 생성 및 기본 세팅
        DataDto dataDto = new DataDto();
        dataDto.setContent("cty");
        dataDto.setTable_num("1");

        // DTO의 timestamp 타입이 Long이므로, 현재 시간을 밀리초로 변환하여 삽입
        dataDto.setTimestamp(System.currentTimeMillis());

        // 2. 내부 data 객체(Map) 생성
        Map<String, Object> innerData = new HashMap<>();
        innerData.put("location", "강원 강릉시");
        // 💡 필요하다면 여기에 사진 URL도 추가로 담아줄 수 있습니다.
        // innerData.put("imageUrl", fullImageUrl);

        dataDto.setData(innerData);

        try {
            // 3. 웹소켓으로 앱(전체)에 동일한 DataDto 포맷 그대로 브로드캐스트
            // WebSocketHandler의 ObjectMapper가 알아서 DataDto를 JSON으로 예쁘게 바꿔줍니다.
            webSocketHandler.broadcast(dataDto);
            System.out.println("[INFO] 시골팀 화재 데이터를 웹소켓으로 브로드캐스트 했습니다.");

            // 4. DB 서버로 데이터 전송 (작성해주신 sendData 메서드 활용!)
            dbServerClient.sendData(dataDto);
            System.out.println("[INFO] 시골팀 화재 데이터를 DB 서버로 전송했습니다.");

        } catch (Exception e) {
            System.err.println("[ERROR] 시골팀 데이터 전송 중 오류 발생: " + e.getMessage());
        }
    }

    // 스마트홈(house) 전용 기존 앱 알림
    private void sendAlertToApp(String teamId, String type, String url, String message) {
        if (!"house".equals(teamId)) {
            return;
        }
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type.contains("fire") ? "FIRE" : "INTRUSION");
        alert.put("imageUrl", url);
        alert.put("message", message);
        alert.put("timestamp", System.currentTimeMillis());
        webSocketHandler.broadcast(alert);
    }
}