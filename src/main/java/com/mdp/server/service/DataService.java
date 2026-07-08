package com.mdp.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

            System.out.println(responseBody);
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

    // 로그인 전용: 성공 여부와 isAdmin 값을 Map으로 반환
    public Map<String, Object> processLogin(DataDto data) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isSuccess", false);
        resultMap.put("isAdmin", 0);

        try {
            String responseBody = dbServerClient.sendData(data);
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                System.out.println("로그인 DB 응답: "+ responseBody);

                // DB가 응답한 {"data": {"success": true, "isAdmin": 0}} 에서 파싱
                boolean isSuccess = root.path("data").path("success").asBoolean(false);
                int isAdmin = root.path("data").path("is_admin").asInt(0);

                resultMap.put("isSuccess", isSuccess);
                resultMap.put("isAdmin", isAdmin);
            }
        } catch (Exception e) {
            System.out.println("로그인 데이터 파싱 실패: " + e.getMessage());
        }
        return resultMap;
    }


    // 테이블 8 전용: 응답에서 message 객체만 추출

    public Map<String, Object> extractMessageFromTable8(DataDto data) {
        try {
            // 1. DB 서버로 요청 전송 및 응답 수신
            String responseBody = dbServerClient.sendData(data);

            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);

                // 2. root -> data -> message 경로로 차례대로 진입
                JsonNode messageNode = root.path("data").path("message");

                // 3. message 노드가 비어있지 않다면 Map 구조로 변환하여 리턴
                if (!messageNode.isMissingNode()) {
                    return objectMapper.convertValue(
                            messageNode,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("테이블 8 message 추출 중 에러 발생: " + e.getMessage());
        }
        return null; // 실패 시 null 반환
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