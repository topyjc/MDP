package com.mdp.server.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WebNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 웹 서버 특정 URL로 낙상 알림 Payload를 HTTP POST 전송
     */
    public void sendWebAlert(Map<String, Object> alertData) {
        try {
            // 💡 실제 웹 서버 브로드캐스트 엔드포인트 URL 설정
            String webAlertUrl = "http://192.168.0.22:3000/api/alerts/broadcast";

            restTemplate.postForEntity(webAlertUrl, alertData, String.class);
            System.out.println("[INFO] 가로등팀 낙상 알림을 웹 서버 URL로 전송했습니다.");
        } catch (Exception e) {
            System.err.println("[ERROR] 웹 서버 알림 전송 실패: " + e.getMessage());
        }
    }
}