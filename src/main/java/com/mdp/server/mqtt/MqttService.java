package com.mdp.server.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.AiServerClient;
import com.mdp.server.client.MediaServerClient;
import com.mdp.server.config.Mqtt;
import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.WebSocketHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MqttService implements MqttCallback {

    private final DataService dataService;
    private final WebSocketHandler webSocketHandler;
    private final MediaServerClient mediaServerClient;
    private final ObjectMapper objectMapper;
    private final Mqtt mqttConfig;
    private final AiServerClient aiServerClient;

    private MqttClient client;

    public MqttService(
            DataService dataService,
            WebSocketHandler webSocketHandler,
            MediaServerClient mediaServerClient,
            ObjectMapper objectMapper,
            Mqtt mqttConfig,
            AiServerClient aiServerClient
    ) {
        this.dataService = dataService;
        this.webSocketHandler = webSocketHandler;
        this.mediaServerClient = mediaServerClient;
        this.objectMapper = objectMapper;
        this.mqttConfig = mqttConfig;
        this.aiServerClient = aiServerClient;
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
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        byte[] payload = message.getPayload();

        if (topic.contains("media")) {

            handleMediaMessage(topic, payload);
        } else {

            handleEventMessage(topic, payload);
        }
    }
    private boolean isMediaTopic(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 6 && "media".equals(parts[4]);
    }

    private void handleMediaMessage(String topic, byte[] payload) {
        // 토픽 분리 (mdp/sensor/조이름/media/파일명)
        String[] topicParts = topic.split("/");
        if (topicParts.length < 6) return;

        String direction = topicParts[1];   // "sensor"
        String teamId = topicParts[2];      // "home" (조이름)
        String deviceName = topicParts[3];  // "esp32-led"
        String dataType = topicParts[4];    // "media"
        String fileName = topicParts[5];    // "fire_image_detection-0.8-2026.jpg"

        // 파일명 분석 (하이픈 '-' 기준)
        String[] fileParts = fileName.split("-");
        if (fileParts.length < 3) return;

        String analysisType = fileParts[1]; // "fire_image_detection"
        double confidence = 0.0;
        try {
            confidence = Double.parseDouble(fileParts[2]);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 확률값 추출 실패: " + fileParts[2]);
        }

        try {
            // 미디어 서버 업로드 (URL)
            String uploadResponseJson = mediaServerClient.uploadImage(teamId, fileName, payload);
            Map<String, String> responseMap = objectMapper.readValue(uploadResponseJson, new TypeReference<Map<String, String>>() {});
            String fullImageUrl = "http://192.168.0.20:8090" + responseMap.get("fileUrl");

            // 확률 기반 판단 로직
            if (confidence >= 0.9) {
                // 확률 0.9 이상 -> AI 판독 없이 즉시 앱 전송
                System.out.println("[ALERT] 확률 : (" + confidence + ") 즉시 알림 발송");
                sendAlertToApp(analysisType, fullImageUrl, "실시간 위험 감지");

            } else if (confidence >= 0.5) {
                //  확률 0.5 ~ 0.9 -> AI 서버에 검증 요청
                System.out.println("[ANALYSIS] 확률 : (" + confidence + "). AI 서버 재검증");
                String aiResult = aiServerClient.requestInference(teamId, analysisType, fullImageUrl, System.currentTimeMillis());

                //
                if (aiResult != null && aiResult.contains("detected=true")) {

                    System.out.println("[ALERT] AI 검증 완료 (위험 확정) 앱 알림 발송");
                    sendAlertToApp(analysisType, fullImageUrl, "AI 분석 결과, 위험이 확인됨");

                } else {
                    System.out.println("[SAFE] AI 검증 결과: 정상 상황 (또는 오탐지)으로 판단됨.");
                }
            } else {
                // 확률 0.5 미만
                System.out.println("[INFO] 낮은 확률 (" + confidence + ")로 인해 알림 생략");
            }

        } catch (Exception e) {
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

        webSocketHandler.broadcast(sensorMessage);

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

    public void publish(String topic, Object payload) {
        try {

            String jsonMessage = objectMapper.writeValueAsString(payload);

            MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
            mqttMessage.setQos(1);

            client.publish(topic, mqttMessage);

            System.out.println("[MQTT 발신 성공] 토픽: " + topic + " | 메시지: " + jsonMessage);

        } catch (Exception e) {
            System.err.println("[MQTT 발신 실패] 토픽: " + topic);
            e.printStackTrace();
        }
    }
}
