package com.mdp.server.client;

import com.mdp.server.dto.DataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${web.server.url}")
    private String dbServerUrl;

    public void sendData(DataDto data) {
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            dbServerUrl + "/data",
                            data,
                            String.class
                    );

            System.out.println("[DB SERVER] response = " + response.getStatusCode());

        } catch (Exception e) {
            System.out.println("[DB SERVER] 데이터 전송 실패");
            e.printStackTrace();
        }
    }
}