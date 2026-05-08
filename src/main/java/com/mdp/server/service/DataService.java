package com.mdp.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    private final DbServerClient dbServerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataService(DbServerClient dbServerClient) {
        this.dbServerClient = dbServerClient;
    }

    public boolean processData(DataDto data) {
        validateData(data);
        setTimestampIfEmpty(data);
        logData(data);

        try {

            String responseBody = dbServerClient.sendData(data);

            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);

                boolean isSuccess = root.path("data").path("success").asBoolean(false);

                return isSuccess;
            }

            return true;

        } catch (Exception e) {
            System.out.println("DB 통신 오류");
            e.printStackTrace();
            return false;
        }
    }

    public DataDto fetchData(String content, String tableNum) {
        return dbServerClient.getDataFromDb(content, tableNum);
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
        System.out.println("data      : " + data.getData());
        System.out.println("timestamp : " + data.getTimestamp());
        System.out.println("================================");
    }
}