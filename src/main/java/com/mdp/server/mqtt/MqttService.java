package com.mdp.server.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.SensorWebSocketHandler;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class MqttService implements MqttCallback {

    private final DataService dataService;
    private final SensorWebSocketHandler sensorWebSocketHandler;
    private final MediaServerClient mediaServerClient;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker}")
    private String brokerUrl;

    @Value("${mqtt.client-id:mdp-main-server}")
    private String clientId;

    @Value("${mqtt.topics:mdp/#}")
    private String subscribeTopic;

    private MqttClient client;

    public MqttService(
            DataService dataService,
            SensorWebSocketHandler sensorWebSocketHandler,
            MediaServerClient mediaServerClient,
            ObjectMapper objectMapper
    ) {
        this.dataService = dataService;
        this.sensorWebSocketHandler = sensorWebSocketHandler;
        this.mediaServerClient = mediaServerClient;
        this.objectMapper = objectMapper;
    }

    public synchronized void connect() {
        try {
            if (client != null && client.isConnected()) {
                System.out.println("[MQTT] already connected");
                return;
            }

            String resolvedClientId = clientId + "-" + UUID.randomUUID();
            client = new MqttClient(brokerUrl, resolvedClientId);
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            client.connect(options);
            client.subscribe(subscribeTopic, 1);

            System.out.println("[MQTT] connected to broker: " + brokerUrl);
            System.out.println("[MQTT] subscribed topic: " + subscribeTopic);
        } catch (MqttException e) {
            System.out.println("[MQTT] connect failed");
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("[MQTT] connection lost");
        if (cause != null) {
            cause.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        try {
            byte[] payload = mqttMessage.getPayload();

            System.out.println("[MQTT] topic = " + topic);
            System.out.println("[MQTT] payload bytes = " + payload.length);

            if (isMediaTopic(topic)) {
                handleMediaMessage(topic, payload);
            } else {
                handleEventMessage(topic, payload);
            }
        } catch (Exception e) {
            System.out.println("[MQTT] message processing failed");
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // subscribe consumer only, no-op
    }

    private boolean isMediaTopic(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 6 && "media".equals(parts[4]);
    }

    private void handleMediaMessage(String topic, byte[] payload) {
        String[] parts = topic.split("/");

        // mdp/{group}/{deviceType}/{deviceId}/media/{fileName}
        String group = parts[1];
        String fileName = parts[5];

        System.out.println("[MQTT][MEDIA] group = " + group);
        System.out.println("[MQTT][MEDIA] fileName = " + fileName);

        String uploadedUrl = mediaServerClient.uploadImage(group, fileName, payload);

        System.out.println("[MQTT][MEDIA] uploaded to media server: " + uploadedUrl);
    }

    private void handleEventMessage(String topic, byte[] payload) {
        String payloadText = new String(payload, StandardCharsets.UTF_8);
        System.out.println("[MQTT][EVENT] payload = " + payloadText);

        DataDto dataDto = mapToDataDto(payloadText);

        dataService.processData(dataDto);

        SensorMessage sensorMessage = new SensorMessage(
                dataDto.getContent(),
                dataDto.getTable_num(),
                dataDto.getData(),
                dataDto.getTimestamp()
        );

        sensorWebSocketHandler.broadcast(sensorMessage);

        System.out.println("[MQTT][EVENT] DB save + WebSocket broadcast completed");
    }

    private DataDto mapToDataDto(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {}
            );

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
        } catch (Exception e) {
            throw new RuntimeException("Invalid event JSON payload: " + json, e);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long parseTimestamp(Object raw) {
        if (raw == null) {
            return System.currentTimeMillis();
        }

        if (raw instanceof Number number) {
            return number.longValue();
        }

        if (raw instanceof String str) {
            String trimmed = str.trim();

            if (trimmed.isEmpty()) {
                return System.currentTimeMillis();
            }

            // 숫자 문자열
            if (trimmed.matches("^\\d+$")) {
                return Long.parseLong(trimmed);
            }

            // "yyyy-MM-dd HH:mm:ss"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(trimmed, formatter);

            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        return System.currentTimeMillis();
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("[MQTT] disconnected");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void publish(String topic, String payload) {
        try {
            if (client == null || !client.isConnected()) {
                throw new IllegalStateException("MQTT client is not connected");
            }

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);

            client.publish(topic, message);

            System.out.println("[MQTT][PUBLISH] topic = " + topic);
            System.out.println("[MQTT][PUBLISH] payload = " + payload);
        } catch (Exception e) {
            System.out.println("[MQTT][PUBLISH] failed");
            e.printStackTrace();
            throw new RuntimeException("MQTT publish failed", e);
        }
    }
}