package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Collections;

@Component
public class DbServerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${db.server.url}")
    private String dbServerUrl;

    // 🟢 [기존 코드] 데이터를 DB 서버로 전송 (POST)
    public void sendData(DataDto data) {
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(dbServerUrl + "/data", data, String.class);
            System.out.println("[DB SERVER] POST response = " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("[DB SERVER] 데이터 전송 실패");
            e.printStackTrace();
        }
    }

    // 🔵 [신규 코드] DB 서버에서 데이터를 가져옴 (GET)
    public List<DataDto> getDataFromDb(String content) {
        try {
            // DB 서버의 GET API 주소 구성 (예: http://db-server-ip:port/data?content=road)
            String url = dbServerUrl + "/data?content=" + content;

            // RestTemplate의 exchange 메서드를 사용하여 List 형태로 데이터 받기
            ResponseEntity<List<DataDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DataDto>>() {}
            );

            System.out.println("[DB SERVER] GET 조회 성공 - content: " + content);
            return response.getBody();

        } catch (Exception e) {
            System.out.println("[DB SERVER] 데이터 수신 실패: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList(); // 에러 시 빈 리스트 반환 방어코드
        }
    }
}