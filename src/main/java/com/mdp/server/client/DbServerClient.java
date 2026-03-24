package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DbServerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String DB_SERVER_URL = "http://localhost:7000/data";

    public void sendData(DataDto data) {

        try {

            System.out.println("[DB] 데이터 보내는 중");
            System.out.println("[DB] " + data);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            DB_SERVER_URL,
                            data,
                            String.class
                    );

            System.out.println("[DB] response status = " + response.getStatusCode());

        } catch (Exception e) {

            System.out.println("[DB] 전송 실패");
            e.printStackTrace();

        }

    }
}