package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

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

    public List<String> getGuardianIds(String wardId) {
        List<String> guardianIds = new ArrayList<>();

        try {
            String url = dbServerUrl + "/api/relation/guardian?wardId=" + wardId;

            // 1. DB 서버 응답 수신
            String responseJson = restTemplate.getForObject(url, String.class);

            if (responseJson == null || responseJson.trim().isEmpty()) {
                return guardianIds; // 빈 리스트 반환
            }

            // 2. JSON 파싱
            JsonNode rootNode = objectMapper.readTree(responseJson);
            boolean isSuccess = rootNode.path("success").asBoolean(false);

            // 3. 성공 시 guardianIds 배열 순회
            if (isSuccess) {
                JsonNode arrayNode = rootNode.path("guardianIds");

                if (arrayNode.isArray()) {
                    for (JsonNode node : arrayNode) {
                        guardianIds.add(node.asText()); // ["csp", "user456"] 등의 원소들을 리스트에 추가
                    }
                }
                System.out.println("[INFO] 보호자 리스트 조회 성공! (" + wardId + " -> " + guardianIds + ")");

            } else {
                String message = rootNode.path("message").asText("이유 알 수 없음");
                System.out.println("[INFO] 보호자 조회 실패 (" + wardId + "): " + message);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 보호자 정보 조회 중 예외 발생: " + e.getMessage());
        }

        return guardianIds; // 보호자 ID들의 리스트 반환
    }
}