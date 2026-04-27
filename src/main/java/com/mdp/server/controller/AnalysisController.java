package com.mdp.server.controller;

import com.mdp.server.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/alert")
public class AnalysisController {

    private final WebSocketHandler webSocketHandler; // 앱 통신용 (직접 만드신 클래스)
    private final RestTemplate restTemplate = new RestTemplate(); // 웹 서버 통신용

    // application.properties에 설정해둘 웹 서버 주소
    @Value("${web.server.url}")
    private String webServerUrl;

    public AnalysisController(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * DB 서버 전용 API: DB 서버가 분석/위험 데이터를 여기로 쏴줍니다.
     */
    @PostMapping("/receive")
    public ResponseEntity<String> pushDataFromDb(@RequestBody Map<String, Object> analyzedData) {
        System.out.println("[DB서버 -> 메인서버] 분석 데이터 수신: " + analyzedData);

        // -----------------------------------------------------
        // 1. 앱으로 전송 (WebSocket)
        // -----------------------------------------------------
        try {
            // WebSocketHandler의 broadcast(Object)가 Map을 JSON으로 변환해서 쏴줍니다!
            webSocketHandler.broadcast(analyzedData);
            System.out.println("[메인서버 -> 앱] 웹소켓 브로드캐스트 완료");
        } catch (Exception e) {
            System.out.println("[메인서버 -> 앱] 웹소켓 전송 실패: " + e.getMessage());
        }

        // -----------------------------------------------------
        // 2. 웹 서버로 전송 (HTTP POST)
        // -----------------------------------------------------
        try {
            // 웹 서버가 이 데이터를 받을 수 있는 창구(API) 주소로 POST 요청을 보냅니다.
            restTemplate.postForEntity(webServerUrl + "/api/analysis-data", analyzedData, String.class);
            System.out.println("[메인서버 -> 웹서버] POST 전송 완료");
        } catch (Exception e) {
            System.out.println("[메인서버 -> 웹서버] POST 전송 실패: " + e.getMessage());
        }

        // DB 서버에게 "양쪽 모두 잘 전달했어!" 라고 응답
        return ResponseEntity.ok("앱(WebSocket) 및 웹 서버(POST)로 데이터 중계 완료!");
    }
}