package com.mdp.server.service;

import com.mdp.server.mqtt.MqttService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeviceControlService {

    private final MqttService mqttService;

    public DeviceControlService(@Lazy MqttService mqttService) {
        this.mqttService = mqttService;
    }


     // 도로팀: 긴급 차량 감지 시 신호등 제어

    public void sendTrafficLightCommand() {
        String topic = "mdp/control/road/esp8266-trafficLight/event";
        Map<String, Object> payload = createBasePayload("traffic_light", "emergency", "");
        mqttService.publish(topic, payload);
    }

    // 스마트홈팀 : LED 점멸

    // 화재 감지 - LED1, LED2 각각 BLINK-FIRE 명령 발행

    public void sendFireAlertLeds() {
        String led1_topic = "mdp/control/house/esp32-led1/event";
        String led2_topic = "mdp/control/house/esp32-led2/event";

        // LED1 발행
        Map<String, Object> payloadLed1 = createBasePayload("led1", "BLINK-FIRE", "");
        mqttService.publish(led1_topic, payloadLed1);

        // LED2 발행
        Map<String, Object> payloadLed2 = createBasePayload("led2", "BLINK-FIRE", "");
        mqttService.publish(led2_topic, payloadLed2);

        System.out.println("화재 LED1, LED2 점멸");
    }


     // 가스 누출 - LED1, LED2 각각 BLINK-GAS 명령 발행

    public void sendGasAlertLeds() {
        String led1_topic = "mdp/control/house/esp32-led1/event";
        String led2_topic = "mdp/control/house/esp32-led2/event";

        // LED1 발행
        Map<String, Object> payloadLed1 = createBasePayload("led1", "BLINK-GAS", "");
        mqttService.publish(led1_topic, payloadLed1);

        // LED2 발행
        Map<String, Object> payloadLed2 = createBasePayload("led2", "BLINK-GAS", "");
        mqttService.publish(led2_topic, payloadLed2);

        System.out.println("가스 LED1, LED2 점멸");
    }

    private Map<String, Object> createBasePayload(String target, String action, String value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", target);
        payload.put("action", action);
        payload.put("value", value);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}