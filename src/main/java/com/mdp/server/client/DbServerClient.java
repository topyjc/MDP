package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DbServerClient {

    private final RestTemplate restTemplate = new RestTemplate();

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
            // DB 서버의 보호자 조회 API 주소 (DB 팀과 맞춘 URL로 수정)
            String url = dbServerUrl + "/api/relation/guardian?wardId=" + wardId;

            // 응답으로 보호자의 userId(String)를 바로 반환한다고 가정
            String guardianId = restTemplate.getForObject(url, String.class);
            return guardianId;
        } catch (Exception e) {
            System.err.println("[ERROR] 보호자 정보 조회 실패: " + e.getMessage());
            return null;
        }
    }
}