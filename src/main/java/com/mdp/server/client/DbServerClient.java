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
    private String dbServerUrl; // application.properties에 설정된 DB 서버 주소

    /**
     * [DB 서버로 데이터 저장 - String 반환 기능 추가]
     */
    public String sendData(DataDto data) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    dbServerUrl + "/data",
                    data,
                    String.class
            );
            System.out.println("[DB SERVER 전송] POST response = " + response.getStatusCode());

            // 🔥 DB 서버가 보내준 JSON 응답(예: {"success": true})을 그대로 반환합니다!
            return response.getBody();

        } catch (Exception e) {
            System.out.println("[DB SERVER 전송 실패] " + e.getMessage());
            // 에러가 났을 때는 null을 반환해서, DataService가 "아! 실패했구나(success: false)"라고 판단하게 만듭니다.
            return null;
        }
    }

    /**
     * [DB 서버에서 데이터 가져오기 - 신규 추가 기능]
     * DB 서버의 GET API를 호출하여 데이터를 List 형태로 받아옵니다.
     */
    public DataDto getDataFromDb(String content, String tableNum) {
        try {
            String url = dbServerUrl + "/data?content=" + content + "&table_num=" + tableNum;

            // DB 서버에서 단일 객체 받아오기
            DataDto responseData = restTemplate.getForObject(url, DataDto.class);

            // 포장 없이 그냥 날것 그대로 반환!
            return responseData;

        } catch (Exception e) {
            System.out.println("[DB SERVER 조회 실패] " + e.getMessage());
            return null; // 에러가 나거나 데이터가 없으면 깔끔하게 null 반환
        }
    }
}