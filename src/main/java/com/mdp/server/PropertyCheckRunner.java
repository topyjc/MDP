package com.mdp.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PropertyCheckRunner implements CommandLineRunner {

    @Value("${mqtt.broker-url:NOT_FOUND}")
    private String brokerUrl;

    @Value("${mqtt.client-id:NOT_FOUND}")
    private String clientId;

    @Value("${mqtt.topics:NOT_FOUND}")
    private String topic;

    @Override
    public void run(String... args) {
        System.out.println("[CHECK] mqtt.broker-url = " + brokerUrl);
        System.out.println("[CHECK] mqtt.client-id = " + clientId);
        System.out.println("[CHECK] mqtt.topics = " + topic);
    }
}