package com.mdp.server.dto;

public class DataDto {

    private String project;
    private String component;
    private Object value;
    private long timestamp;

    // getter

    public String getProject() {
        return project;
    }

    public String getComponent() {
        return component;
    }

    public Object getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // setter

    public void setProject(String project) {
        this.project = project;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}