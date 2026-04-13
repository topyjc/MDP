package com.mdp.server.controller;

import com.mdp.server.dto.DeviceControlDto;
// import com.mdp.server.service.MqttService; // 나중에 만들 MQTT 발신 서비스
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
public class DeviceControlController {

    // private final MqttService mqttService;

    // 의존성 주입 (나중에 MqttService가 완성되면 주석 해제)
    // public DeviceControlController(MqttService mqttService) {
    //     this.mqttService = mqttService;
    // }

    @PostMapping("/control")
    public ResponseEntity<String> controlDevice(@RequestBody DeviceControlDto request) {

        System.out.println("[API 수신] 타겟: " + request.getTarget() + ", 동작: " + request.getAction() + ", 값: " + request.getValue());

        // 타겟(기기)에 따른 라우팅 분기 처리
        switch (request.getTarget().toUpperCase()) {
            case "LED1":
            case "LED2":
                // TODO: MqttService를 통해 밝기값(request.getValue()) Publish
                System.out.println("-> " + request.getTarget() + " 밝기를 " + request.getValue() + "(으)로 조절합니다.");
                break;

            case "GAS":
            case "FAN":
            case "INTRUSION":
                // TODO: MqttService를 통해 ON/OFF(request.getAction()) Publish
                System.out.println("-> " + request.getTarget() + " 기기를 " + request.getAction() + " 상태로 변경합니다.");
                break;

            case "OTP":
                // TODO: MqttService를 통해 도어락 토픽으로 OTP(request.getValue()) Publish
                System.out.println("-> 도어락으로 OTP [" + request.getValue() + "] 를 전송합니다.");
                break;

            case "QR":
                // TODO: DB에 사용자 정보와 기기(request.getValue()) 매핑 정보 저장 (MQTT 전송 안함!)
                System.out.println("-> DB에 새로운 기기 [" + request.getValue() + "] 를 등록합니다.");
                break;

            default:
                return ResponseEntity.badRequest().body("알 수 없는 타겟입니다: " + request.getTarget());
        }

        // 앱에게 '명령을 서버가 잘 받았음'을 알림 (HTTP 200 OK)
        return ResponseEntity.ok("명령 처리 완료: " + request.getTarget());
    }
}