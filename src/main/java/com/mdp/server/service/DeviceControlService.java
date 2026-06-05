package com.mdp.server.service;

import com.mdp.server.mqtt.MqttService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeviceControlService {

    private final MqttService mqttService;

    // 순환 참조를 방지하기 위해 @Lazy를 사용 (MqttService와 서로 참조하기 때문)
    public DeviceControlService(@Lazy MqttService mqttService) {
        this.mqttService = mqttService;
    }


//     // 가로등팀: 긴급 차량 감지 시 신호등 제어
//
//    public void sendTrafficLightCommand(String teamId, String deviceName, String action) {
//        String topic = "mdp/control/" + teamId + "/" + deviceName;
//        Map<String, Object> payload = createPayload("traffic_light", action);
//
//        mqttService.publish(topic, payload);
//    }


//     // 화재 또는 가스 누출 시 LED 둘 다 점멸 제어
//
//    public void sendLedBlinkCommand(String teamId, String deviceName) {
//        String topic = "mdp/control/" + teamId + "/" + deviceName;
//        Map<String, Object> payload = createPayload("led", "BLINK_ALL"); // action에 점멸(BLINK_ALL) 추가
//
//        System.out.println("[긴급 제어] " + teamId + " 조의 기기(" + deviceName + ")에 LED 점멸 명령을 전송합니다.");
//        mqttService.publish(topic, payload);
//    }


     // 공통 JSON 데이터 생성 메서드

    private Map<String, Object> createPayload(String target, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", target);
        payload.put("action", action);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}