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

    private final WebSocketHandler webSocketHandler;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${web.server.url}")
    private String webServerUrl;

    public AnalysisController(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping("/receive")
    public ResponseEntity<String> pushDataFromDb(@RequestBody Map<String, Object> analyzedData) {
        System.out.println("분석 데이터 수신 : " + analyzedData);

        // 앱 전송
        try {
            webSocketHandler.broadcast(analyzedData);
            System.out.println("분석 데이터 앱 전송 완료");
        } catch (Exception e) {
            System.out.println("분석 데이터 앱 전송 실패: " + e.getMessage());
        }

        // 웹 전송
        try {
            restTemplate.postForEntity(webServerUrl + "/api/analysis-data", analyzedData, String.class);
            System.out.println("분석 데이터 웹서버 전송 완료");
        } catch (Exception e) {
            System.out.println("분석 데이터 웹서버 전송 실패: " + e.getMessage());
        }

        // DB 서버에게 "양쪽 모두 잘 전달했어!" 라고 응답
        return ResponseEntity.ok("전송 완료");
    }
}