package com.mdp.server.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.config.Mqtt;
import com.mdp.server.handler.MediaEventHandler;
import com.mdp.server.handler.SensorEventHandler;
import com.mdp.server.handler.WardEventHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MqttService implements MqttCallback {

    private final SensorEventHandler sensorEventHandler;
    private final MediaEventHandler mediaEventHandler;
    private final WardEventHandler wardEventHandler;
    private final ObjectMapper objectMapper;
    private final Mqtt mqttConfig;

    private MqttClient client;

    public MqttService(
            SensorEventHandler sensorEventHandler,
            MediaEventHandler mediaEventHandler,
            WardEventHandler wardEventHandler,
            ObjectMapper objectMapper,
            Mqtt mqttConfig
    ) {
        this.sensorEventHandler = sensorEventHandler;
        this.mediaEventHandler = mediaEventHandler;
        this.wardEventHandler = wardEventHandler;
        this.objectMapper = objectMapper;
        this.mqttConfig = mqttConfig;
    }

    public synchronized void connect() {
        System.out.println("### MQTT CONNECT BEGIN ###");
        String brokerUrl = mqttConfig.getBrokerUrl();
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalStateException("MQTT broker URL is null or blank");
        }

        try {
            if (client != null && client.isConnected()) return;

            String baseClientId = (mqttConfig.getClientId() == null || mqttConfig.getClientId().isBlank())
                    ? "mdp-main-server" : mqttConfig.getClientId();
            String subscribeTopic = (mqttConfig.getTopics() == null || mqttConfig.getTopics().isEmpty())
                    ? "mdp/#" : mqttConfig.getTopics().get(0);
            String resolvedClientId = baseClientId + "-" + UUID.randomUUID();

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

            client.connect(options);
            client.subscribe(subscribeTopic, mqttConfig.getQos());
            System.out.println("[MQTT] 연결 성공 및 토픽 구독 완료: " + subscribeTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("[MQTT] Connection Lost");
        if (throwable != null) throwable.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        byte[] payload = message.getPayload();

        if (topic.contains("media")) {
            mediaEventHandler.handle(topic, payload);

        } else if (topic.contains("cty") || topic.contains("streetlight")) {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String tableNum = jsonNode.path("table_num").asText("");

            // 1. 시골팀(cty) 이면서 테이블 번호가 "2" 또는 "3" 인가?
            boolean isCtyEmergency = topic.contains("cty") && (tableNum.equals("2") || tableNum.equals("3"));

            // 2. 가로등팀(streetlight) 이면서 테이블 번호가 "0" 인가?
            boolean isStreetlightEmergency = topic.contains("streetlight") && tableNum.equals("0");
            // ------------------------------------------

            System.out.println(isCtyEmergency);

            if (isCtyEmergency || isStreetlightEmergency) {
                String payloadStr = new String(payload);
                wardEventHandler.processEmergency(payloadStr);
            } else {
                // 조건에 안 맞으면(일반 데이터면) 기존 센서 핸들러로 전달
                sensorEventHandler.handle(topic, payload);
            }

        } else {
            // 3. 그 외 아예 다른 토픽인 경우 (일반 센서 부서)
            System.out.println("일반 센서");
            sensorEventHandler.handle(topic, payload);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}

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