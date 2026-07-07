package com.mdp.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.service.DeviceControlService;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class SensorEventHandler {

    private final DataService dataService;
    private final DeviceControlService controlService;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final Executor mqttEventExecutor;

    public SensorEventHandler(
            DataService dataService,
            DeviceControlService controlService,
            WebSocketHandler webSocketHandler,
            ObjectMapper objectMapper,
            @Qualifier("mqttEventExecutor") Executor mqttEventExecutor
    ) {
        this.dataService = dataService;
        this.controlService = controlService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
        this.mqttEventExecutor = mqttEventExecutor;
    }

    public void handle(String topic, byte[] payload) {
        // 수신 스레드가 블로킹되지 않도록 모든 작업을 전용 스레드풀로 위임
        // (ForkJoinPool.commonPool 대신 mediaEventHandler와 동일한 bounded pool 공유:
        //  블로킹 I/O를 commonPool에 태우지 않기 위함 + 리소스 상한 관리 일원화)
        mqttEventExecutor.execute(() -> {
            try {
                String payloadText = new String(payload, StandardCharsets.UTF_8);
                System.out.println("[MQTT][EVENT] 일반 센서 수신: " + payloadText);

                // 1. DB 저장을 위한 DTO 맵핑 및 HTTP 전송
                DataDto dataDto = mapToDataDto(payloadText);
                dataService.processData(dataDto);

                // 2. 웹소켓 실시간 앱/웹 브로드캐스트
                SensorMessage sensorMessage = new SensorMessage(
                        dataDto.getContent(),
                        dataDto.getTable_num(),
                        dataDto.getData(),
                        dataDto.getTimestamp()
                );
                webSocketHandler.broadcast(sensorMessage);

                // 3. 스마트홈 팀(house) 가스 누출(gasSw == 1) 즉시 체크
                if ("house".equals(dataDto.getContent())) {
                    Map<String, Object> sensorData = dataDto.getData();

                    if (sensorData != null && sensorData.containsKey("gasSw")) {
                        Object gasSwVal = sensorData.get("gasSw");

                        // 문자열로 비교
                        if (gasSwVal != null && "1".equals(String.valueOf(gasSwVal))) {
                            System.out.println("가스 누출 발생 (gasSw = 1)");
                            controlService.sendGasAlertLeds();
                        }
                    }
                }

                System.out.println("[MQTT][EVENT] 비동기 처리 완료 (DB + 웹소켓 + 기기제어)");
            } catch (Exception e) {
                System.err.println("[ERROR] 일반 센서 비동기 처리 중 오류 발생");
                e.printStackTrace();
            }
        });
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

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