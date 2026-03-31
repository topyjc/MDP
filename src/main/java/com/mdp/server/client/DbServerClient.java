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
    // 반환 타입을 List가 아니라 그냥 DataDto 하나로 바꿉니다!
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