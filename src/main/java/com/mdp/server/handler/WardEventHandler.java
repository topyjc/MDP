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
            // 1. JSON 최상단 파싱
            JsonNode root = objectMapper.readTree(payload);
            String tableNum = root.path("table_num").asText();

            // 💡 2. data 객체 내부 접근 (userId, location 등이 들어있음)
            JsonNode dataNode = root.path("data");
            String wardId = dataNode.path("userId").asText();
            String location = dataNode.path("location").asText("위치 정보 없음");

            if (wardId.isEmpty()) {
                System.out.println("[WARN] JSON 내부 data.userId가 비어있습니다.");
                return;
            }

            // 💡 3. table_num에 따른 위험 상황 텍스트 세팅
            String eventTitle = "";
            if ("2".equals(tableNum)) {
                eventTitle = "차량 전복 사고";
            } else if ("3".equals(tableNum)) {
                double powerUsage = dataNode.path("power_usage").asDouble(0.0);
                eventTitle = "전력 사용량 이상 감지 (" + powerUsage + "kWh)";
            } else if ("0".equals(tableNum)) {
                eventTitle = "가로등 긴급 상황 감지";
            } else {
                eventTitle = "피보호자 긴급 상황 발생";
            }

            // 4. DB 서버에서 보호자 ID 조회 (아까 수정한 메서드 호출)
            String guardianId = dbServerClient.getGuardianId(wardId);

            // 5. 보호자가 존재하면 1:1 타겟팅 웹소켓 알림 전송
            if (guardianId != null && !guardianId.isEmpty()) {
                System.out.println("[ALERT] 피보호자(" + wardId + ") 위험 감지 -> 보호자(" + guardianId + ")에게 알림 전송");

                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "WARD_EMERGENCY");
                alert.put("wardId", wardId);
                alert.put("eventTitle", eventTitle);
                alert.put("location", location);
                alert.put("message", "피보호자(" + wardId + ")님에게 [" + eventTitle + "]가 발생했습니다. (위치: " + location + ")");
                alert.put("timestamp", System.currentTimeMillis());

                // 보호자에게 1:1 전송
                webSocketHandler.sendToSpecificUser(guardianId, alert);
            } else {
                System.out.println("[INFO] " + wardId + "에 매핑된 보호자가 없어 알림을 보낼 수 없습니다.");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 피보호자 이벤트 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}