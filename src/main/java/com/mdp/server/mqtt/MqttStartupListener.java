package com.mdp.server.mqtt;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MqttStartupListener {

    private final MqttService mqttService;

    public MqttStartupListener(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * Spring Boot 기동이 끝난 뒤 MQTT 연결 시작
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        System.out.println("[MQTT] ApplicationReadyEvent fired");
        mqttService.connect();
    }
}