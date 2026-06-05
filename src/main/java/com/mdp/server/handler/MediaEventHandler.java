package com.mdp.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.AiServerClient;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.service.DeviceControlService;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MediaEventHandler {

    private final MediaServerClient mediaServerClient;
    private final AiServerClient aiServerClient;
    private final DeviceControlService controlService;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public MediaEventHandler(
            MediaServerClient mediaServerClient,
            AiServerClient aiServerClient,
            DeviceControlService controlService,
            WebSocketHandler webSocketHandler,
            ObjectMapper objectMapper
    ) {
        this.mediaServerClient = mediaServerClient;
        this.aiServerClient = aiServerClient;
        this.controlService = controlService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void handle(String topic, byte[] payload) {
        String[] topicParts = topic.split("/");
        if (topicParts.length < 6) return;

        String teamId = topicParts[2];      // 조이름
        String deviceName = topicParts[3];  // 기기이름
        String fileName = topicParts[5];    // 파일명

        String[] fileParts = fileName.split("-");
        if (fileParts.length < 3) return;

        String analysisType = fileParts[1]; // "fire_image_detection" 또는 "emergency_vehicle" 등
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
                sendAlertToApp(analysisType, fullImageUrl, "실시간 위험 감지");
                isDangerDetected = true;

            } else if (confidence >= 0.5) {
                System.out.println("[ANALYSIS] 모호한 확률 (" + confidence + ") -> AI 서버 재검증 요청");
                String aiResult = aiServerClient.requestInference(teamId, analysisType, fullImageUrl, System.currentTimeMillis());

                if (aiResult != null && aiResult.contains("detected=true")) {
                    System.out.println("[ALERT] AI가 위험 상황 최종 확정함");
                    sendAlertToApp(analysisType, fullImageUrl, "AI 분석 결과, 위험 상황 확정");
                    isDangerDetected = true;
                } else {
                    System.out.println("[SAFE] AI 검증 결과: 정상(오탐)으로 판단됨");
                }
            } else {
                System.out.println("[INFO] 무시할만한 낮은 확률 (" + confidence + ") 처리 생략");
            }

//            // 4. [비즈니스 로직] 위험 감지 시 가로등/화재 제어 명령 연동
//            if (isDangerDetected) {
//                // Case A: 소방차/구급차 등 긴급 차량 감지 시 -> 가로등 신호등을 파란불(GREEN)로 제어
//                if (analysisType.contains("emergency") || "streetlamp".equals(teamId)) {
//                    controlService.sendTrafficLightCommand(teamId, deviceName, "GREEN");
//                }
//
//                // Case B: 화재 이미지 감지 시 -> LED 둘 다 점멸 제어
//                if (analysisType.contains("fire")) {
//                    controlService.sendLedBlinkCommand(teamId, deviceName);
//                }
//            }

        } catch (Exception e) {
            System.err.println("[ERROR] 미디어 파일 처리 오류");
            e.printStackTrace();
        }
    }

    private void sendAlertToApp(String type, String url, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type.contains("fire") ? "FIRE" : "INTRUSION");
        alert.put("imageUrl", url);
        alert.put("message", message);
        alert.put("timestamp", System.currentTimeMillis());
        webSocketHandler.broadcast(alert);
    }
}