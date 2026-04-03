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
        // 1. AI 팀이 요구한 규격대로 JSON Body 만들기
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("team_id", teamId);
        requestBody.put("analysis_type", analysisType);
        requestBody.put("image_url", imageUrl);         // 실제 파일이 아닌 미디어 서버의 URL

        // 2. HTTP 헤더 설정 (JSON 타입 명시)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. 편지 봉투에 담기
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. AI 서버로 POST 발사! (엔드포인트는 AI 팀과 맞춘 주소로 변경하세요 예: /api/analyze)
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServerUrl + "/api/analyze",
                    requestEntity,
                    Map.class
            );

            // 5. AI 서버의 답변 꺼내기 (예: "FIRE", "NORMAL" 등)
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("result")) {
                return String.valueOf(responseBody.get("result"));
            }
            return "UNKNOWN";

        } catch (Exception e) {
            System.out.println("[AI SERVER] AI 분석 요청 실패: " + e.getMessage());
            return "ERROR";
        }
    }
}