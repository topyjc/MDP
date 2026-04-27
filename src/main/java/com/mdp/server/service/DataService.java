package com.mdp.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    private final DbServerClient dbServerClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 🔥 JSON 파싱을 위한 도구 추가

    public DataService(DbServerClient dbServerClient) {
        this.dbServerClient = dbServerClient;
    }

    /**
     * [데이터 저장/검증 흐름]
     * 기기(ESP32) -> MQTT 브로커 -> MqttService -> 현재 메서드 호출
     * 앱/웹 -> UserController(로그인/회원가입) -> 현재 메서드 호출 -> 성공 여부 반환
     */
    public boolean processData(DataDto data) { // 🔥 리턴 타입을 void에서 boolean으로 변경
        validateData(data);
        setTimestampIfEmpty(data);
        logData(data);

        try {
            // 1. DB 서버로 데이터 전송 및 응답 받아오기
            // 🚨 (주의: DbServerClient.sendData() 메서드가 DB 서버의 응답(String)을  반환하도록설정되어 있어야 합니다!)
            String responseBody = dbServerClient.sendData(data);

            // 2. DB 서버가 응답을 주면 JSON을 분석해서 "success" 값을 확인
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("success").asBoolean(false); // success 필드가 없거나 false면 false 반환
            }

            // 단순 센서 데이터 저장(MQTT) 등 응답이 중요하지 않은 경우는 true로 간주
            return true;

        } catch (Exception e) {
            System.out.println("[DB 통신/파싱 오류] " + e.getMessage());
            return false; // 파싱 실패나 통신 오류 시 false 반환
        }
    }

    /**
     * [데이터 조회 흐름]
     * 웹/앱 -> DataController -> 현재 메서드 호출 -> DB 서버에서 가져오기
     */
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