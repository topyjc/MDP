package com.mdp.server.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.MediaUploadResponse;
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
    private final MediaServerClient mediaServerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MqttClient client;

    public MqttService(
            DataService dataService,
            SensorWebSocketHandler sensorWebSocketHandler,
            MediaServerClient mediaServerClient
    ) {
        this.dataService = dataService;
        this.sensorWebSocketHandler = sensorWebSocketHandler;
        this.mediaServerClient = mediaServerClient;
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
                        if (isMediaTopic(receivedTopic)) {
                            handleImageMessage(receivedTopic, message.getPayload());
                        } else {
                            handleJsonMessage(receivedTopic, message);
                        }
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

    private boolean isMediaTopic(String receivedTopic) {
        String[] parts = receivedTopic.split("/");
        return parts.length >= 6 && "media".equals(parts[4]);
    }

    private void handleImageMessage(String receivedTopic, byte[] imageBytes) {
        String[] parts = receivedTopic.split("/");

        String group = parts[1];
        String fileName = parts[5];

        System.out.println("[MQTT-IMAGE] group = " + group);
        System.out.println("[MQTT-IMAGE] fileName = " + fileName);
        System.out.println("[MQTT-IMAGE] size = " + imageBytes.length + " bytes");

        MediaUploadResponse response = mediaServerClient.uploadImage(imageBytes, fileName, group);

        System.out.println("[MEDIA SERVER] upload response = " + response.getMessage());
        System.out.println("[MEDIA SERVER] saved fileName = " + response.getFileName());
        System.out.println("[MEDIA SERVER] fileUrl = " + response.getFileUrl());
    }

    private void handleJsonMessage(String receivedTopic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        System.out.println("[MQTT] ===== MESSAGE ARRIVED =====");
        System.out.println("[MQTT] received topic = " + receivedTopic);
        System.out.println("[MQTT] payload = " + payload);
        System.out.println("[MQTT] qos = " + message.getQos());
        System.out.println("[MQTT] retained = " + message.isRetained());

        DataDto data = mapToDataDto(payload);

        dataService.processData(data);

        SensorMessage sensorMessage = new SensorMessage(
                data.getContent(),
                data.getTable_num(),
                data.getData(),
                data.getTimestamp()
        );

        sensorWebSocketHandler.broadcast(sensorMessage);
    }

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

            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
            }

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