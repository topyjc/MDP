package com.mdp.server.mqtt;

import com.mdp.server.config.Mqtt;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService {

    private final Mqtt mqtt;
    private MqttClient client;

    public MqttService(Mqtt mqtt) {
        this.mqtt = mqtt;
    }

    @PostConstruct
    public void connect() {
        try {
            System.out.println("[MQTT] connect() started");
            System.out.println("[MQTT] brokerUrl = " + mqtt.getBrokerUrl());
            System.out.println("[MQTT] clientId = " + mqtt.getClientId());
            System.out.println("[MQTT] qos = " + mqtt.getQos());
            System.out.println("[MQTT] topics = " + mqtt.getTopics());

            client = new MqttClient(
                    mqtt.getBrokerUrl(),
                    mqtt.getClientId(),
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(10);

            if (mqtt.getUsername() != null && !mqtt.getUsername().isBlank()) {
                options.setUserName(mqtt.getUsername());
            }

            if (mqtt.getPassword() != null && !mqtt.getPassword().isBlank()) {
                options.setPassword(mqtt.getPassword().toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[MQTT] connection lost -> " +
                            (cause != null ? cause.getMessage() : "unknown"));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    System.out.println("========== MQTT MESSAGE ==========");
                    System.out.println("topic   : " + topic);
                    System.out.println("payload : " + payload);
                    System.out.println("qos     : " + message.getQos());
                    System.out.println("==================================");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(options);
            System.out.println("[MQTT] connected -> " + client.isConnected());

            List<String> topics = mqtt.getTopics();

            if (topics == null || topics.isEmpty()) {
                System.out.println("[MQTT] topics is empty");
                return;
            }

            for (String topic : topics) {
                System.out.println("[MQTT] subscribing -> " + topic);
                client.subscribe(topic, mqtt.getQos());
                System.out.println("[MQTT] subscribed -> " + topic);
            }

        } catch (Exception e) {
            System.out.println("[MQTT] connect failed -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            System.out.println("[MQTT] disconnect error -> " + e.getMessage());
        }
    }
}