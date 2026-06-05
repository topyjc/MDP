package com.mdp.server.controller;

import com.mdp.server.service.DeviceControlService;
import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/alert")
public class AnalysisController {

    private final WebSocketHandler webSocketHandler;
    private final DeviceControlService controlService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${web.server.url}")
    private String webServerUrl;

    public AnalysisController(WebSocketHandler webSocketHandler, DeviceControlService controlService) {
        this.webSocketHandler = webSocketHandler;
        this.controlService = controlService;
    }

    @PostMapping("/receive")
    public ResponseEntity<String> pushDataFromDb(@RequestBody Map<String, Object> analyzedData) {
        System.out.println("분석 데이터 수신 : " + analyzedData);

        try {
            webSocketHandler.broadcast(analyzedData);
            System.out.println("분석 데이터 앱 전송 완료");
        } catch (Exception e) {
            System.out.println("분석 데이터 앱 전송 실패: " + e.getMessage());
        }

        try {
            restTemplate.postForEntity(webServerUrl + "/api/analysis-data", analyzedData, String.class);
            System.out.println("분석 데이터 웹서버 전송 완료");
        } catch (Exception e) {
            System.out.println("분석 데이터 웹서버 전송 실패: " + e.getMessage());
        }

        // 3. 기기(ESP32) 제어 명령 (MQTT 전송)
        try {
            // DB 서버가 보내는 데이터 구조에 맞춰 조건을 걸어줍니다. (예: type이 GAS_LEAK 인지 확인)
            // ⚠️ 주의: 아래 "type"이나 "gas"는 임시로 작성한 것이니 DB 서버의 실제 데이터에 맞게 수정하세요.
            Object alertType = analyzedData.get("type");

            if (alertType != null && alertType.toString().contains("gas")) {
                System.out.println("[💥 DB 서버 알림] 가스 누출 확정! ESP32 기기 제어 명령을 내립니다.");

                // 제어할 조 이름(teamId) 추출. 만약 DB 서버가 teamId를 안 보내준다면 추가해 달라고 요청해야 합니다!
                String teamId = analyzedData.getOrDefault("teamId", "home").toString();
                String deviceName = "esp32-led"; // 제어할 기기명

                controlService.sendLedBlinkCommand(teamId, deviceName);
            }
        } catch (Exception e) {
            System.out.println("기기 제어 명령(MQTT) 전송 실패: " + e.getMessage());
        }

        // DB 서버에게 "양쪽 모두 잘 전달했어!" 라고 응답
        return ResponseEntity.ok("전송 완료");
    }
}