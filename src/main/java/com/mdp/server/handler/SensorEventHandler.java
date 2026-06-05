package com.mdp.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.service.DeviceControlService;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class SensorEventHandler {

    private final DataService dataService;
    private final DeviceControlService controlService;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public SensorEventHandler(
            DataService dataService,
            DeviceControlService controlService,
            WebSocketHandler webSocketHandler,
            ObjectMapper objectMapper
    ) {
        this.dataService = dataService;
        this.controlService = controlService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void handle(String topic, byte[] payload) {
        try {
            String payloadText = new String(payload, StandardCharsets.UTF_8);
            System.out.println("[MQTT][EVENT] 일반 센서 수신: " + payloadText);

            // 1. DB 저장을 위한 DTO 맵핑 및 저장
            DataDto dataDto = mapToDataDto(payloadText);
            dataService.processData(dataDto);

            // 2. 웹소켓 실시간 앱 브로드캐스트 * 웹은 풀링
            SensorMessage sensorMessage = new SensorMessage(
                    dataDto.getContent(),
                    dataDto.getTable_num(),
                    dataDto.getData(),
                    dataDto.getTimestamp()
            );
            webSocketHandler.broadcast(sensorMessage);

//            // 3. [비즈니스 로직] 가스 누출 감지 시 제어 명령 작동
//            if ("gas".equals(dataDto.getContent())) {
//                Map<String, Object> sensorData = dataDto.getData();
//                Object gasValueObj = sensorData.get("value"); // ⚠️ 하드웨어가 보내는 키값("value" 등)에 맞추세요.
//
//                if (gasValueObj != null) {
//                    double gasValue = Double.parseDouble(gasValueObj.toString());
//
//                    // 가스 누출 임계치 판단 (예: 50.0 초과 시 누출로 판단)
//                    if (gasValue > 50.0) {
//                        System.out.println("[위험] 가스 누출 수치 감지 (" + gasValue + ") -> 시스템 개입 시작");
//
//                        String teamId = extractTeamId(topic);
//                        controlService.sendLedBlinkCommand(teamId, "esp32-led");
//                    }
//                }
//            }

            System.out.println("[MQTT][EVENT] DB 저장 + 웹소켓 전송 완료");
        } catch (Exception e) {
            System.err.println("[ERROR] 일반 센서 처리 중 오류 발생");
            e.printStackTrace();
        }
    }

    private String extractTeamId(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 3 ? parts[2] : "unknown";
    }

    private DataDto mapToDataDto(String json) throws Exception {
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        DataDto dto = new DataDto();
        dto.setContent(asString(map.get("content")));
        dto.setTable_num(asString(map.get("table_num")));
        dto.setTimestamp(parseTimestamp(map.get("timestamp")));

        Object dataObj = map.get("data");
        if (dataObj instanceof Map<?, ?> rawMap) {
            dto.setData((Map<String, Object>) rawMap);
        } else {
            throw new IllegalArgumentException("data field must be an object");
        }
        return dto;
    }

    private String asString(Object value) { return value == null ? null : String.valueOf(value); }

    private long parseTimestamp(Object raw) {
        if (raw == null) return System.currentTimeMillis();
        if (raw instanceof Number number) return number.longValue();
        if (raw instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return System.currentTimeMillis();
            if (trimmed.matches("^\\d+$")) return Long.parseLong(trimmed);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(trimmed, formatter);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return System.currentTimeMillis();
    }
}