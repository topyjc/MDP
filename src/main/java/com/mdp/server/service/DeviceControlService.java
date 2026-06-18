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

    public void sendTrafficLightCommand(String teamId,  String action) {
        String topic = "mdp/control/" + teamId + "/" + "esp8266-trafficLight" + "/" + "event";
        Map<String, Object> payload = createBasePayload("traffic_light", action, "");
        mqttService.publish(topic, payload);
    }

    // [화재 감지] LED1, LED2 각각 BLINK-FIRE 명령 발행

    public void sendFireAlertLeds(String teamId, String deviceName) {
        String topic = "mdp/control/" + teamId + "/" + deviceName;

        // LED1 발행
        Map<String, Object> payloadLed1 = createBasePayload("led1", "BLINK-FIRE", "");
        mqttService.publish(topic, payloadLed1);

        // LED2 발행
        Map<String, Object> payloadLed2 = createBasePayload("led2", "BLINK-FIRE", "");
        mqttService.publish(topic, payloadLed2);

        System.out.println("화재 LED1, LED2 점멸");
    }


     // [가스 누출] LED1, LED2 각각 BLINK-GAS 명령 발행

    public void sendGasAlertLeds(String teamId, String deviceName) {
        String topic = "mdp/control/" + teamId + "/" + deviceName;

        // LED1 발행
        Map<String, Object> payloadLed1 = createBasePayload("led1", "BLINK-GAS", "");
        mqttService.publish(topic, payloadLed1);

        // LED2 발행
        Map<String, Object> payloadLed2 = createBasePayload("led2", "BLINK-GAS", "");
        mqttService.publish(topic, payloadLed2);

        System.out.println("[가스 긴급 제어] " + teamId + " 조에 LED1, LED2 점멸(BLINK-GAS) 명령을 각각 발행.");
    }

    //공통 JSON 페이로드 생성 헬퍼 메서드 ("value" 필드 추가)

    private Map<String, Object> createBasePayload(String target, String action, String value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", target);
        payload.put("action", action);
        payload.put("value", value); // 👈 요청하신 빈 문자열("") 또는 특정 값이 들어가는 곳
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}