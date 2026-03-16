package com.mdp.server.service;

import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    private final DbServerClient dbServerClient;

    public DataService(DbServerClient dbServerClient) {
        this.dbServerClient = dbServerClient;
    }

    public void processData(DataDto data) {

        validateData(data);

        setTimestampIfEmpty(data);

        logData(data);

        dbServerClient.sendData(data);

    }

    private void validateData(DataDto data) {

        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        if (data.getProject() == null || data.getProject().isBlank()) {
            throw new IllegalArgumentException("project 없음");
        }

        if (data.getComponent() == null || data.getComponent().isBlank()) {
            throw new IllegalArgumentException("component 없음");
        }

    }

    private void setTimestampIfEmpty(DataDto data) {

        if (data.getTimestamp() == 0) {
            data.setTimestamp(System.currentTimeMillis());
        }

    }

    private void logData(DataDto data) {

        System.out.println("========= DEVICE DATA =========");
        System.out.println("project   : " + data.getProject());
        System.out.println("component : " + data.getComponent());
        System.out.println("value     : " + data.getValue());
        System.out.println("timestamp : " + data.getTimestamp());
        System.out.println("================================");

    }

}