package com.mdp.server.dto;

import java.util.Map;

public class SensorMessage {

    private String content;
    private String table_num;
    private Long timestamp;
    private Map<String, Object> data;

    public SensorMessage() {
    }

    public SensorMessage(String content, String table_num, Map<String, Object> data, Long timestamp) {
        this.content = content;
        this.table_num = table_num;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String Content) {
        this.content = content;
    }

    public String getTable_num() {
        return table_num;
    }

    public void setTable_num(String table_num) {
        this.table_num = table_num;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}