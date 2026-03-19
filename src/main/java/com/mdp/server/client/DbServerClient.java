    package com.mdp.server.client;

    import com.mdp.server.dto.DataDto;
    import org.springframework.http.ResponseEntity;
    import org.springframework.stereotype.Component;
    import org.springframework.web.client.RestTemplate;

    @Component
    public class DbServerClient {

        private final RestTemplate restTemplate = new RestTemplate();

        private final String DB_SERVER_URL = "http://localhost:8081/data";

        public void sendData(DataDto data) {

            try {

                ResponseEntity<String> response =
                        restTemplate.postForEntity(
                                DB_SERVER_URL,
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