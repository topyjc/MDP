package com.mdp.server.mqtt;

import com.mdp.server.mqtt.MqttService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MqttStartupListener {

    private final MqttService mqttService;

    public MqttStartupListener(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        mqttService.connect();
    }
}