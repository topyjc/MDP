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

        if (data.getContent() == null || data.getContent().isBlank()) {
            throw new IllegalArgumentException("content 없음");
        }

        if (data.getTable_num() == null || data.getTable_num().isBlank()) {
            throw new IllegalArgumentException("table_num 없음");
        }

    }

    private void setTimestampIfEmpty(DataDto data) {

        if (data.getTimestamp() == 0) {
            data.setTimestamp(System.currentTimeMillis());
        }

    }

    private void logData(DataDto data) {

        System.out.println("========= DEVICE DATA =========");
        System.out.println("content   : " + data.getContent());
        System.out.println("table_num : " + data.getTable_num());
        System.out.println("data     : " + data.getData());
        System.out.println("timestamp : " + data.getTimestamp());
        System.out.println("================================");

    }

}