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

    public String requestInference(String teamId, String analysisType, String imageUrl, long timestamp) {
        //JSON Body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("team_id", teamId);
        requestBody.put("analysis_type", analysisType);
        requestBody.put("image_url", imageUrl);         // URL

        //HTTP 헤더 설정 (JSON 타입)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServerUrl + "/api/analyze",
                    requestEntity,
                    Map.class
            );

            //AI 서버 답변
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("result")) {
                return String.valueOf(responseBody.get("result"));
            }
            return "UNKNOWN";

        } catch (Exception e) {
            System.out.println("AI SERVER 분석 요청 실패: " + e.getMessage());
            return "ERROR";
        }
    }
}