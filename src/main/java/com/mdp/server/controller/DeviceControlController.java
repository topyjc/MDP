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

    public DeviceControlController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/control")
    public ResponseEntity<String> controlDevice(@RequestBody DeviceControlDto request) {

        System.out.println("API 수신 타겟: " + request.getTarget() + ", 동작: " + request.getAction() + ", 값: " + request.getValue());

        // JSON 변환
        Map<String, String> commandData = new HashMap<>();
        commandData.put("action", request.getAction());
        if (request.getValue() != null) {
            commandData.put("value", request.getValue());
        }
        
        switch (request.getTarget().toUpperCase()) {
            case "LED1":
            case "LED2":
                mqttService.publish("mdp/control/house/esp32-led/event", commandData);
                break;
            case "GAS":
            case "FAN":
                mqttService.publish("mdp/control/house/esp32-gas/event", commandData);
                break;
            case "INTRUSION":
                mqttService.publish("mdp/control/house/laptop-cam/event", commandData);
                break;
            case "OTP":
                mqttService.publish("mdp/control/house/esp32-doorlock/event", commandData);
                break;
            default:
                return ResponseEntity.badRequest().body("알 수 없는 타겟입니다: " + request.getTarget());
        }

        return ResponseEntity.ok("명령 처리 완료: " + request.getTarget());
    }
}