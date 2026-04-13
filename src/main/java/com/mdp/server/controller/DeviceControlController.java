package com.mdp.server.controller;

import com.mdp.server.dto.DeviceControlDto;
import com.mdp.server.mqtt.MqttService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceControlController {

    private final MqttService mqttService;

    // 의존성 주입 완료!
    public DeviceControlController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/control")
    public ResponseEntity<String> controlDevice(@RequestBody DeviceControlDto request) {

        System.out.println("[API 수신] 타겟: " + request.getTarget() + ", 동작: " + request.getAction() + ", 값: " + request.getValue());

        // 하드웨어로 보낼 데이터 맵 (JSON으로 변환됨)
        Map<String, String> commandData = new HashMap<>();
        commandData.put("action", request.getAction());
        if (request.getValue() != null) {
            commandData.put("value", request.getValue());
        }
        
        switch (request.getTarget().toUpperCase()) {
            case "LED1":
                mqttService.publish("mdp/house/led1/control", commandData);
                break;
            case "LED2":
                mqttService.publish("mdp/house/led2/control", commandData);
                break;
            case "GAS":
                mqttService.publish("mdp/house/gas/control", commandData);
                break;
            case "FAN":
                mqttService.publish("mdp/house/fan/control", commandData);
                break;
            case "INTRUSION":
                    mqttService.publish("mdp/house/intrusion/control", commandData);
                break;
            case "OTP":
                mqttService.publish("mdp/house/doorlock/otp", commandData);
                break;
            case "QR":
                // QR은 DB 저장 로직이 들어가야 하므로 일단 보류!
                System.out.println("-> DB에 새로운 기기 등록 진행...");
                break;
            default:
                return ResponseEntity.badRequest().body("알 수 없는 타겟입니다: " + request.getTarget());
        }

        return ResponseEntity.ok("명령 처리 완료: " + request.getTarget());
    }
}