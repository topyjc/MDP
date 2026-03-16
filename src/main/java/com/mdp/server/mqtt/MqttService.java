package com.mdp.server.mqtt;

import com.mdp.server.config.Mqtt;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MqttService {

    private final Mqtt mqtt;
    private MqttAsyncClient client;

    public MqttService(Mqtt mqtt) {
        this.mqtt = mqtt;
    }

    @PostConstruct
    public void connect() {
        try {
            client = new MqttAsyncClient(
                    mqtt.getBrokerUrl(),
                    mqtt.getClientId(),
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(10);

            if (mqtt.getUsername() != null && !mqtt.getUsername().isBlank()) {
                options.setUserName(mqtt.getUsername());
            }

            if (mqtt.getPassword() != null && !mqtt.getPassword().isBlank()) {
                options.setPassword(mqtt.getPassword().toCharArray());
            }

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println("[MQTT] connected -> " + serverURI + " / reconnect=" + reconnect);
                    subscribeTopics();
                }

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

            client.connect(options).waitForCompletion();
            System.out.println("[MQTT] connect success");

        } catch (MqttException e) {
            throw new RuntimeException("MQTT 연결 실패", e);
        }
    }

    private void subscribeTopics() {
        try {
            if (client == null || !client.isConnected()) {
                return;
            }

            for (String topic : mqtt.getTopics()) {
                client.subscribe(topic, mqtt.getQos()).waitForCompletion();
                System.out.println("[MQTT] subscribed -> " + topic);
            }

        } catch (MqttException e) {
            System.out.println("[MQTT] subscribe error -> " + e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect().waitForCompletion();
            }
            if (client != null) {
                client.close();
            }
        } catch (MqttException e) {
            System.out.println("[MQTT] disconnect error -> " + e.getMessage());
        }
    }
}