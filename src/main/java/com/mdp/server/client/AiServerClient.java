package com.mdp.server.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class AiServerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.server.url}")
    private String aiServerUrl;

    // AI 서버에 판독을 요청하는 메서드
    public String requestInference(String fileUrl, String group) {
        // 1. AI 서버로 보낼 JSON 데이터 만들기
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("imageUrl", fileUrl);
        requestBody.put("group", group); // 예: "living_room_cam"

        // 2. 헤더 설정 (JSON으로 보낸다고 명시)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. 요청 패키지 포장
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. AI 서버로 POST 요청 발사!
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServerUrl + "/api/predict", // AI 팀 API 주소로 변경 필요
                    requestEntity,
                    Map.class
            );

            // 5. AI 서버의 답변(예: "FIRE", "NORMAL", "INTRUDER") 꺼내기
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("result")) {
                return (String) responseBody.get("result");
            }
            return "UNKNOWN";

        } catch (Exception e) {
            System.out.println("[AI SERVER] 요청 실패: " + e.getMessage());
            return "ERROR";
        }
    }
}