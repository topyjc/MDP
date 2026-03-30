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
    private String dbServerUrl; // application.properties에 설정된 DB 서버 주소

    /**
     * [DB 서버로 데이터 저장 - 기존 완료된 기능]
     */
    public void sendData(DataDto data) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    dbServerUrl + "/data",
                    data,
                    String.class
            );
            System.out.println("[DB SERVER 전송] POST response = " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("[DB SERVER 전송 실패] " + e.getMessage());
        }
    }

    /**
     * [DB 서버에서 데이터 가져오기 - 신규 추가 기능]
     * DB 서버의 GET API를 호출하여 데이터를 List 형태로 받아옵니다.
     */
    public List<DataDto> getDataFromDb(String content, String tableNum) {
        try {
            // 1. DB 서버에게 요청할 주소 만들기 (쿼리 파라미터로 조건 전달)
            // 예: http://localhost:9090/data?content=스마트홈&=table_num1
            String url = dbServerUrl + "/data?content=" + content + "&table_num=" + tableNum;

            // 2. RestTemplate을 사용해 GET 요청을 보내고 List<DataDto> 타입으로 응답받기
            ResponseEntity<List<DataDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DataDto>>() {}
            );

            return response.getBody(); // 정상적으로 받아온 데이터 리스트 반환

        } catch (Exception e) {
            System.out.println("[DB SERVER 조회 실패] " + e.getMessage());
            return Collections.emptyList(); // 서버 통신 에러 시 프로그램이 죽지 않도록 빈 리스트 반환
        }
    }
}