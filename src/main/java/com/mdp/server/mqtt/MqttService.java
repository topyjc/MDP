package com.mdp.server.mqtt;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.WebSocketPublisher;
import com.mdp.server.websocket.dto.SensorMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
    private final WebSocketPublisher webSocketPublisher;

    private MqttClient client;

    public MqttService(DataService dataService, WebSocketPublisher webSocketPublisher) {
        this.dataService = dataService;
        this.webSocketPublisher = webSocketPublisher;
    }

    public void connect() {
        try {
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

                        DataDto data = mapToDataDto(receivedTopic, payload);
                        dataService.processData(data);

                        SensorMessage wsMessage = new SensorMessage(
                                data.getProject(),
                                data.getComponent(),
                                data.getValue(),
                                data.getTimestamp() == 0 ? System.currentTimeMillis() : data.getTimestamp()
                        );

                        webSocketPublisher.publishSensorData(wsMessage);

                    } catch (Exception e) {
                        System.out.println("[MQTT] 처리 실패");
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(options);
            client.subscribe(topic, qos);

        } catch (Exception e) {
            System.out.println("[MQTT] connect failed -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DataDto mapToDataDto(String receivedTopic, String payload) {
        String[] parts = receivedTopic.split("/");

        if (parts.length < 5) {
            throw new IllegalArgumentException("토픽 형식이 예상과 다름: " + receivedTopic);
        }

        DataDto data = new DataDto();
        data.setProject(parts[0]);
        data.setComponent(parts[4]);
        data.setValue(parsePayloadValue(payload));
        return data;
    }

    private Object parsePayloadValue(String payload) {
        String trimmed = payload.trim();

        try {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }
}