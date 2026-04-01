package com.mdp.server.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.config.Mqtt;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.SensorWebSocketHandler;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
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
    private final Mqtt mqttConfig;

    private MqttClient client;

    public MqttService(
            DataService dataService,
            SensorWebSocketHandler sensorWebSocketHandler,
            MediaServerClient mediaServerClient,
            ObjectMapper objectMapper,
            Mqtt mqttConfig
    ) {
        this.dataService = dataService;
        this.sensorWebSocketHandler = sensorWebSocketHandler;
        this.mediaServerClient = mediaServerClient;
        this.objectMapper = objectMapper;
        this.mqttConfig = mqttConfig;
    }

    public synchronized void connect() {
        System.out.println("### MQTT CONNECT BEGIN ###");
        System.out.println("brokerUrl = " + mqttConfig.getBrokerUrl());
        System.out.println("clientId = " + mqttConfig.getClientId());
        System.out.println("topics = " + mqttConfig.getTopics());

        String brokerUrl = mqttConfig.getBrokerUrl();
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalStateException("MQTT broker URL is null or blank");
        }

        try {
            if (client != null && client.isConnected()) {
                System.out.println("[MQTT] already connected");
                return;
            }

            String baseClientId = (mqttConfig.getClientId() == null || mqttConfig.getClientId().isBlank())
                    ? "mdp-main-server"
                    : mqttConfig.getClientId();

            String subscribeTopic = (mqttConfig.getTopics() == null || mqttConfig.getTopics().isEmpty())
                    ? "mdp/#"
                    : mqttConfig.getTopics().get(0);

            String resolvedClientId = baseClientId + "-" + UUID.randomUUID();

            System.out.println("[MQTT] preparing client");
            System.out.println("[MQTT] baseClientId = " + baseClientId);
            System.out.println("[MQTT] subscribeTopic = " + subscribeTopic);
            System.out.println("[MQTT] resolvedClientId = " + resolvedClientId);

            client = new MqttClient(brokerUrl, resolvedClientId);
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            if (mqttConfig.getUsername() != null && !mqttConfig.getUsername().isBlank()) {
                options.setUserName(mqttConfig.getUsername());
            }
            if (mqttConfig.getPassword() != null && !mqttConfig.getPassword().isBlank()) {
                options.setPassword(mqttConfig.getPassword().toCharArray());
            }

            System.out.println("[MQTT] connecting...");
            client.connect(options);

            System.out.println("[MQTT] connect success, subscribing...");
            client.subscribe(subscribeTopic, mqttConfig.getQos());

            System.out.println("[MQTT] connected to broker: " + brokerUrl);
            System.out.println("[MQTT] subscribed topic: " + subscribeTopic);
            System.out.println("[MQTT] actual clientId: " + resolvedClientId);
            System.out.println("[MQTT] isConnected after subscribe = " + client.isConnected());
        } catch (MqttException e) {
            System.out.println("[MQTT] connect failed");
            System.out.println("[MQTT] brokerUrl = " + brokerUrl);
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("[MQTT] connection lost");
        if (client != null) {
            System.out.println("[MQTT] client.isConnected = " + client.isConnected());
        }
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        try {
            byte[] payload = mqttMessage.getPayload();

            System.out.println("### MESSAGE ARRIVED HIT ###");
            System.out.println("[MQTT] topic = " + topic);
            System.out.println("[MQTT] qos = " + mqttMessage.getQos());
            System.out.println("[MQTT] retained = " + mqttMessage.isRetained());
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
    private boolean isMediaTopic(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 6 && "media".equals(parts[4]);
    }

    private void handleMediaMessage(String topic, byte[] payload) {
        String[] parts = topic.split("/");

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

            if (trimmed.matches("^\\d+$")) {
                return Long.parseLong(trimmed);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(trimmed, formatter);

            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        return System.currentTimeMillis();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void publish(String topic, String payload) {
        try {
            if (client == null || !client.isConnected()) {
                throw new IllegalStateException("MQTT client is not connected");
            }

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(mqttConfig.getQos());
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

    // 나머지 messageArrived / publish / parse 로직은 기존 그대로 유지
}