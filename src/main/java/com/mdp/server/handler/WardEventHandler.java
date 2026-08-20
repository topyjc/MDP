package com.mdp.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WardEventHandler {

    private final DbServerClient dbServerClient;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public WardEventHandler(DbServerClient dbServerClient, WebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.dbServerClient = dbServerClient;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void processEmergency(String payload) {
        try {
            // 1. ESP32가 보낸 JSON 데이터 파싱
            JsonNode data = objectMapper.readTree(payload);
            String wardId = data.path("userId").asText(); // 피보호자 ID (예: ward123)
            String eventType = data.path("event").asText(); // 이벤트 종류 (예: FALL_DOWN)

            // 2. DB 서버에서 보호자 ID 조회
            String guardianId = dbServerClient.getGuardianId(wardId);

            if (guardianId != null && !guardianId.isEmpty()) {
                System.out.println("[ALERT] " + wardId + "의 보호자(" + guardianId + ")에게 알림을 전송합니다.");

                // 3. 웹소켓으로 보낼 알림 메시지 생성
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "WARD_EMERGENCY");
                alert.put("targetUser", guardianId); // 🎯 수신자 지정
                alert.put("message", "피보호자에게 위험 상황(" + eventType + ")이 발생했습니다!");
                alert.put("timestamp", System.currentTimeMillis());

                // 4. 보호자에게 웹소켓 전송
                webSocketHandler.sendToSpecificUser(guardianId, alert);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 피보호자 이벤트 처리 중 오류 발생");
        }
    }
}