package com.mdp.server.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DbClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String DB_SERVER = "http://localhost:8081";

    public String getUsers() {
        return restTemplate.getForObject(
                DB_SERVER + "/users",
                String.class
        );
    }

    public String saveUser(Object user) {
        return restTemplate.postForObject(
                DB_SERVER + "/users",
                user,
                String.class
        );
    }
}