package com.mdp.server.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private MqttClient client;

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
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("[MQTT] ===== MESSAGE ARRIVED =====");
                    System.out.println("[MQTT] received topic = " + topic);
                    System.out.println("[MQTT] payload = " + payload);
                    System.out.println("[MQTT] qos = " + message.getQos());
                    System.out.println("[MQTT] retained = " + message.isRetained());
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
}