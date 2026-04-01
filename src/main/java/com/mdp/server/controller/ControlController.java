package com.mdp.server.controller;

import com.mdp.server.dto.ControlDto;
import com.mdp.server.mqtt.MqttService; // 기존에 만들어둔 MqttService
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 앱이나 웹에서 접근할 수 있게 허용
public class ControlController {

    private final MqttService mqttService;

    // 생성자로 MqttService 연결
    public ControlController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/control")
    public ResponseEntity<String> controlDevice(@RequestBody ControlDto controlDto) {
        System.out.println("[제어 명령 수신] 타겟: " + controlDto.getTarget());
        System.out.println("[제어 명령 내용] " + controlDto.getCommand());

        // 1. MQTT로 보낼 토픽 이름 만들기 (예: "house/control")
        // ESP32 친구들이 어떤 토픽을 구독(Subscribe)하고 있는지에 맞춰서 "/control" 부분을 수정하면 됩니다!
        String topic = controlDto.getTarget() + "/control";

        // 2. Map으로 들어온 명령을 JSON 문자열로 변환
        String messagePayload = controlDto.getCommandAsJsonString();

        // 3. MQTT로 발행 (Publish)
        mqttService.publish(topic, messagePayload);

        // 4. 앱에게 "명령 잘 전달했어!" 라고 알려주기
        return ResponseEntity.ok("제어 명령이 성공적으로 전달되었습니다.");
    }
}