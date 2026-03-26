package com.mdp.server.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.SensorWebSocketHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class MqttService {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.topics}")
    private String topic;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    private final DataService dataService;
    private final SensorWebSocketHandler sensorWebSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MqttClient client;

    public MqttService(DataService dataService, SensorWebSocketHandler sensorWebSocketHandler) {
        this.dataService = dataService;
        this.sensorWebSocketHandler = sensorWebSocketHandler;
    }

    public void connect() {
        try {
            System.out.println("[MQTT] connect() started");
            System.out.println("[MQTT] brokerUrl = " + brokerUrl);
            System.out.println("[MQTT] clientId = " + clientId);
            System.out.println("[MQTT] qos = " + qos);
            System.out.println("[MQTT] topic = " + topic);

            client = new MqttClient(brokerUrl, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }

            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[MQTT] connection lost: " + cause);
                }

                @Override
                public void messageArrived(String receivedTopic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                        System.out.println("[MQTT] ===== MESSAGE ARRIVED =====");
                        System.out.println("[MQTT] received topic = " + receivedTopic);
                        System.out.println("[MQTT] payload = " + payload);
                        System.out.println("[MQTT] qos = " + message.getQos());
                        System.out.println("[MQTT] retained = " + message.isRetained());

                        // 1. MQTT payload(JSON) -> DataDto
                        DataDto data = mapToDataDto(payload);

                        // 2. DB 서버 전송 포함 내부 처리
                        dataService.processData(data);

                        // 3. WebSocket 전송용 SensorMessage 생성
                        SensorMessage sensorMessage = new SensorMessage(
                                data.getContent(),
                                data.getTable_num(),
                                data.getData(),
                                data.getTimestamp()
                        );

                        // 4. Web/App 실시간 전송
                        sensorWebSocketHandler.broadcast(sensorMessage);

                    } catch (Exception e) {
                        System.out.println("[MQTT] 처리 실패");
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("[MQTT] deliveryComplete called");
                }
            });

            client.connect(options);
            System.out.println("[MQTT] connected = " + client.isConnected());

            client.subscribe(topic, qos);
            System.out.println("[MQTT] subscribed to " + topic);

        } catch (Exception e) {
            System.out.println("[MQTT] connect failed -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * payload 예시
     * {
     *   "content": "road",
     *   "table_num": "1",
     *   "timestamp": "2024-03-24 14:20:00",
     *   "data": {
     *     "carNo": "12가3456"
     *   }
     * }
     */
    private DataDto mapToDataDto(String payload) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(payload, Map.class);

            DataDto data = new DataDto();
            data.setContent(asString(jsonMap.get("content")));
            data.setTable_num(asString(jsonMap.get("table_num")));
            data.setTimestamp(parseTimestamp(jsonMap.get("timestamp")));
            data.setData(parseData(jsonMap.get("data")));

            return data;

        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패: " + payload, e);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long parseTimestamp(Object ts) {
        if (ts == null) {
            return System.currentTimeMillis();
        }

        if (ts instanceof Number number) {
            return number.longValue();
        }

        if (ts instanceof String tsString) {
            String trimmed = tsString.trim();

            // epoch milli 문자열 처리
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
            }

            // "yyyy-MM-dd HH:mm:ss" 형식 처리
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ignored) {
            }
        }

        return System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseData(Object rawData) {
        if (rawData == null) {
            return new HashMap<>();
        }

        if (rawData instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("value", rawData);
        return wrapped;
    }
}