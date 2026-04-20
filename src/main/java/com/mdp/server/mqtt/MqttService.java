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
        // 1. 토픽 분리 (mdp/sensor/조이름/media/파일명)
        String[] topicParts = topic.split("/");
        if (topicParts.length < 5) return;

        String teamId = topicParts[2];
        String fileName = topicParts[5]; // streetlight-fire_image_detection-0.8-20260330...

        // 2. 파일명 상세 분석 (하이픈 '-' 기준)
        String[] fileParts = fileName.split("-");
        if (fileParts.length < 3) return;

        String analysisType = fileParts[1]; // "fire_image_detection"
        double confidence = 0.0;
        try {
            confidence = Double.parseDouble(fileParts[2]); // "0.8" 추출
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 확률값 추출 실패: " + fileParts[2]);
        }

        try {
            // 3. 미디어 서버 업로드 (무조건 실행하여 URL 확보)
            String uploadResponseJson = mediaServerClient.uploadImage(teamId, fileName, payload);
            Map<String, String> responseMap = objectMapper.readValue(uploadResponseJson, new TypeReference<Map<String, String>>() {});
            String fullImageUrl = "http://192.168.0.20:8090" + responseMap.get("fileUrl");

            // 4. 확률 기반 판단 로직 (임계치는 상황에 맞게 조절하세요)
            if (confidence >= 0.9) {
                //  [확실] 확률 0.9 이상 -> AI 판독 없이 즉시 앱 전송
                System.out.println("[ALERT] 확률 : (" + confidence + ") 즉시 알림 발송");
                sendAlertToApp(analysisType, fullImageUrl, "실시간 위험 감지");

            } else if (confidence >= 0.5) {
                //  [애매] 확률 0.5 ~ 0.9 -> AI 서버에 검증 요청
                System.out.println("[ANALYSIS] 확률 : (" + confidence + "). AI 서버 재검증");
                String aiResult = aiServerClient.requestInference(teamId, analysisType, fullImageUrl, System.currentTimeMillis());

                // [초간단 판단] 맨 앞의 detected=true 여부만 확인!
                if (aiResult != null && aiResult.contains("detected=true")) {

                    System.out.println("[ALERT] AI 검증 완료 (위험 확정) 앱 알림 발송");
                    sendAlertToApp(analysisType, fullImageUrl, "AI 분석 결과, 위험이 확인됨");

                } else {

                    // detected=false 이거나, 통신 오류로 이상한 값이 온 경우 모두 무시
                    System.out.println("[SAFE] AI 검증 결과: 정상 상황 (또는 오탐지)으로 판단됨.");
                }
            } else {
                // [무시] 확률 0.5 미만 -> 너무 낮으므로 로그만 남기고 종료
                System.out.println("[INFO] 낮은 확률 (" + confidence + ")로 인해 알림 생략");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 웹소켓을 통해 앱으로 알림(URL 포함)을 쏘는 메서드
    private void sendAlertToApp(String type, String url, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type.contains("fire") ? "FIRE" : "INTRUSION");
        alert.put("imageUrl", url);
        alert.put("message", message);
        alert.put("timestamp", System.currentTimeMillis());

        // WebSocketHandler를 통해 현재 연결된 모든 앱 사용자에게 전송
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

    /**
     * MQTT 브로커로 메시지를 발행(발신)하는 공통 메서드
     */
    public void publish(String topic, Object payload) {
        try {
            // 1. 보낼 데이터를 JSON 문자열로 변환 (하드웨어 기기가 읽기 편하게)
            String jsonMessage = objectMapper.writeValueAsString(payload);

            // 2. MQTT 메시지 객체 생성
            MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
            mqttMessage.setQos(1); // QoS 1: 최소 한 번은 무조건 전달 보장

            // 3. 발신!
            client.publish(topic, mqttMessage);

            System.out.println("[MQTT 발신 성공] 토픽: " + topic + " | 메시지: " + jsonMessage);

        } catch (Exception e) {
            System.err.println("[MQTT 발신 실패] 토픽: " + topic);
            e.printStackTrace();
        }
    }
}

    // 나머지 messageArrived / publish / parse 로직은 기존 그대로 유지
