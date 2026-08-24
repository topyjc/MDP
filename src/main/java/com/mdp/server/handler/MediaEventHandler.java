package com.mdp.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.AiServerClient;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.service.DeviceControlService;
import com.mdp.server.mqtt.MqttService; // 💡 MqttService 임포트
import org.springframework.context.annotation.Lazy;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class MediaEventHandler {

    private final MediaServerClient mediaServerClient;
    private final AiServerClient aiServerClient;
    private final DeviceControlService controlService;
    private final  MqttService mqttService; // 💡 1. MqttService 필드 추가
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final Executor mqttEventExecutor;

    public MediaEventHandler(
            MediaServerClient mediaServerClient,
            AiServerClient aiServerClient,
            DeviceControlService controlService,
            @Lazy MqttService mqttService, // 💡 2. 생성자 주입 추가
            WebSocketHandler webSocketHandler,
            ObjectMapper objectMapper,
            @Qualifier("mqttEventExecutor") Executor mqttEventExecutor
    ) {
        this.mediaServerClient = mediaServerClient;
        this.aiServerClient = aiServerClient;
        this.controlService = controlService;
        this.mqttService = mqttService;
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

        String teamId = topicParts[2];      // 예: "cty"
        String deviceName = topicParts[3];  // 예: "laptop"
        String fileName = topicParts[5];

        String[] fileParts = fileName.split("-");
        if (fileParts.length < 3) return;

        String analysisType = fileParts[1];
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
                }
            }

            // 3. 제어 및 추가 연동
            if (isDangerDetected) {
                if (analysisType.contains("emergency")) {
                    controlService.sendTrafficLightCommand();
                }

                if (analysisType.contains("fire")) {
                    controlService.sendFireAlertLeds();
                }

                // 시골팀(cty) 화재 감지 시 MqttService를 통해 기기로 이벤트 전송
                if ("cty".equals(teamId) && analysisType.contains("fire")) {
                    sendFireEventToDevice(teamId, deviceName);
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 미디어 파일 처리 오류");
            e.printStackTrace();
        }
    }

    // 💡 MqttService의 publish 메서드를 직접 사용하는 방식
    private void sendFireEventToDevice(String teamId, String deviceName) {
        String topic = String.format("mdp/control/%s/%s/event", teamId, deviceName);

        // 1. Map 객체 생성
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("action", "fire_detected");
        payloadMap.put("value", "");

        try {
            // 2. objectMapper로 변환하지 않고, Map 객체를 '그대로' MqttService에 전달!
            mqttService.publish(topic, payloadMap);

            System.out.println("[INFO] MqttService로 화재 감지 이벤트 발행 완료. Topic: " + topic);
        } catch (Exception e) {
            System.err.println("[ERROR] MqttService 발행 실패: " + e.getMessage());
        }
    }

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