package com.mdp.server.service;

import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataService {

    private final DbServerClient dbServerClient;

    public DataService(DbServerClient dbServerClient) {
        this.dbServerClient = dbServerClient;
    }

    /**
     * [데이터 저장 흐름]
     * 기기(ESP32) -> MQTT 브로커 -> MqttService -> 현재 메서드 호출 -> DB 서버로 전송
     */
    public void processData(DataDto data) {
        validateData(data);         // 데이터 검증 (기존 코드 유지)
        setTimestampIfEmpty(data);  // 시간값 세팅 (기존 코드 유지)
        logData(data);              // 로그 출력 (기존 코드 유지)

        // DB 서버 클라이언트를 통해 실제 데이터 전송 (POST)
        dbServerClient.sendData(data);
    }

    /**
     * [데이터 조회 흐름]
     * 웹/앱 -> DataController -> 현재 메서드 호출 -> DB 서버에서 가져오기
     */
    public DataDto fetchData(String content, String tableNum) {
        // DB 클라이언트에게 조건(작품명, 테이블번호)을 주고 찾아오라고 지시
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
        System.out.println("data     : " + data.getData());
        System.out.println("timestamp : " + data.getTimestamp());
        System.out.println("================================");

    }

}