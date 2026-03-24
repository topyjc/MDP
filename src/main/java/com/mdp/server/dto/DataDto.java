package com.mdp.server.dto;

import java.util.Map;

public class DataDto {

    private String content;
    private String table_num;
    private long timestamp;
    private Map<String, Object> data;
    // getter

    public String getContent() {
        return content;
    }

    public String getTable_num() {
        return table_num;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // setter

    public void setContent(String content) {
        this.content = content;
    }

    public void setTable_num(String table_num) {
        this.table_num = table_num;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}