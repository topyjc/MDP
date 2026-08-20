package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DbServerClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${db.server.url}")
    private String dbServerUrl;

    public String sendData(DataDto data) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    dbServerUrl + "/data",
                    data,
                    String.class
            );
            System.out.println("DB SERVER POST response = " + response.getStatusCode());

            return response.getBody();

        } catch (Exception e) {
            System.out.println("DB SERVER 전송 실패 : " + e.getMessage());
            return null;
        }
    }

    public DataDto getDataFromDb(String content, String tableNum) {
        try {
            String url = dbServerUrl + "/data?content=" + content + "&table_num=" + tableNum;

            DataDto responseData = restTemplate.getForObject(url, DataDto.class);

            return responseData;

        } catch (Exception e) {
            System.out.println("DB SERVER 조회 실패 : " + e.getMessage());
            return null;
        }
    }

    // 💡 피보호자 ID로 보호자 ID를 조회하는 메서드
    public String getGuardianId(String wardId) {
        try {
            String url = dbServerUrl + "/api/relation/guardian?wardId=" + wardId;

            // 1. DB 서버의 응답을 일단 String 형태의 JSON으로 받습니다.
            String responseJson = restTemplate.getForObject(url, String.class);

            if (responseJson == null || responseJson.trim().isEmpty()) {
                return null;
            }

            // 2. ObjectMapper를 통해 JSON 상자를 엽니다.
            JsonNode rootNode = objectMapper.readTree(responseJson);

            // 3. "success" 키의 값을 확인합니다. (없으면 기본값 false)
            boolean isSuccess = rootNode.path("success").asBoolean(false);

            // 4. 성공 시와 실패 시 로직 분기
            if (isSuccess) {
                String guardianId = rootNode.path("guardianId").asText();
                System.out.println("[INFO] 보호자 조회 성공! (" + wardId + " -> " + guardianId + ")");
                return guardianId;
            } else {
                // 실패 시 DB 서버가 보내준 message를 로그로 남깁니다.
                String message = rootNode.path("message").asText("이유 알 수 없음");
                System.out.println("[INFO] 보호자 조회 실패 (" + wardId + "): " + message);
                return null; // 실패했으므로 null 반환 (웹소켓 알림이 가지 않음)
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 보호자 정보 조회 중 네트워크 또는 파싱 예외 발생: " + e.getMessage());
            return null;
        }
    }
}