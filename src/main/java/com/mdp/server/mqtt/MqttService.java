package com.mdp.server.mqtt;

import com.mdp.server.dto.DataDto;
import com.mdp.server.dto.SensorMessage;
import com.mdp.server.service.DataService;
import com.mdp.server.websocket.SensorWebSocketHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import java.nio.charset.StandardCharsets;
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

                        DataDto data = mapToDataDto(receivedTopic, payload);
                        dataService.processData(data);

                        long timestamp = data.getTimestamp() == 0
                                ? System.currentTimeMillis()
                                : data.getTimestamp();

                        SensorMessage wsMessage = new SensorMessage(
                                data.getContent(),
                                data.getTable_num(),
                                data.getData(),
                                timestamp
                        );

                        sensorWebSocketHandler.broadcast(wsMessage);

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

    private DataDto mapToDataDto(String receivedTopic, String payload) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(payload, Map.class);

            DataDto data = new DataDto();

            data.setContent((String) jsonMap.get("content"));
            data.setTable_num((String) jsonMap.get("table_num"));

            Object ts = jsonMap.get("timestamp");
            if (ts != null) {
                data.setTimestamp(((Number) ts).longValue());
            } else {
                data.setTimestamp(System.currentTimeMillis());
            }

            data.setData((Map<String, Object>) jsonMap.get("data"));

            return data;

        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패: " + payload, e);
        }
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