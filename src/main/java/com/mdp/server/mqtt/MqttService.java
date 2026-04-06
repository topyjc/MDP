package com.mdp.server.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.AiServerClient;
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
    private final AiServerClient aiServerClient;

    private MqttClient client;

    public MqttService(
            DataService dataService,
            SensorWebSocketHandler sensorWebSocketHandler,
            MediaServerClient mediaServerClient,
            ObjectMapper objectMapper,
            Mqtt mqttConfig,
            AiServerClient aiServerClient
    ) {
        this.dataService = dataService;
        this.sensorWebSocketHandler = sensorWebSocketHandler;
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
        // ... (기존 로그 출력 코드들) ...

        byte[] payload = message.getPayload();

        // 💡 여기가 핵심: 토픽 이름에 "media"가 포함되어 있으면 미디어 처리 로직으로!
        if (topic.contains("media")) {
            // 미디어 파일(이미지 등) HTTP 전송 로직 호출
            handleMediaMessage(topic, payload);
        } else {
            // 기존의 일반 센서 JSON 데이터 처리 로직 호출
            handleEventMessage(topic, payload);
        }
    }
    private boolean isMediaTopic(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 6 && "media".equals(parts[4]);
    }

    private void handleMediaMessage(String topic, byte[] payload) {
        // 1. 토픽 분리 (예: mdp/streetlight/media/streetlight-fire-20260330-193015-123.jpg)
        String[] topicParts = topic.split("/");

        // 토픽 길이 검증 (최소 4칸은 되어야 함)
        if (topicParts.length < 4) {
            System.out.println("[MQTT] wr: " + topic);
            return;
        }

        String teamId = topicParts[1];       // "streetlight"
        String dataType = topicParts[2];     // "media"
        String fileName = topicParts[3];     // "streetlight-fire-20260330-193015-123.jpg"

        // 2. 파일명에서 분석 유형(analysisType) 추출
        // 하이픈(-) 기준으로 자르기
        String[] fileParts = fileName.split("-");

        // 최소 조이름과 분석유형은 있어야 하므로 검증
        if (fileParts.length < 2) {
            System.out.println("[MQTT] 파일명 형식이 맞지 않습니다: " + fileName);
            return;
        }

        // 파일명의 두 번째 덩어리가 분석 유형
        String analysisType = fileParts[1];  // "fire"
        long currentTimestamp = System.currentTimeMillis(); // 타임스탬프는 메인 서버 시간

        System.out.println("[MQTT][MEDIA] Team: " + teamId + " | Type: " + analysisType);
        System.out.println("[MQTT][MEDIA] File: " + fileName + " (" + payload.length + " bytes)");

        try {
            // 1. 미디어 서버 업로드 (현재 반환값이 JSON 문자열임)
            String uploadResponseJson = mediaServerClient.uploadImage(teamId, fileName, payload);
            System.out.println("[MQTT][MEDIA] media answer: " + uploadResponseJson);

            // 2. JSON에서 fileUrl만 추출 (ObjectMapper 사용)
            Map<String, String> responseMap = objectMapper.readValue(
                    uploadResponseJson,
                    new TypeReference<Map<String, String>>() {}
            );
            String relativeUrl = responseMap.get("fileUrl"); // "/media/files/..."

            // 3. AI 서버가 접근할 수 있도록 완전한 HTTP 주소로 조립!
            // (주의: 192.168.x.x 부분과 포트를 실제 미디어 서버 주소로 변경하세요!)
            String mediaServerBaseUrl = "http://192.168.0.20:8090";
            String fullImageUrl = mediaServerBaseUrl + relativeUrl;

            System.out.println("[MQTT][MEDIA] AI final URL: " + fullImageUrl);

            // 4. AI 서버에 판독 요청 (풀 URL을 보냅니다)
            String aiResult = aiServerClient.requestInference(teamId, analysisType, fullImageUrl, currentTimestamp);
            System.out.println("[AI] final result: " + aiResult);

        } catch (Exception e) {
            System.out.println("[MQTT][MEDIA] media error: " + e.getMessage());
            e.printStackTrace();
        }
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