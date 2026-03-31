package com.mdp.server.dto;

public class SensorMessage {

    private String project;
    private String component;
    private Object value;
    private long timestamp;

    public SensorMessage() {
    }

    public SensorMessage(String project, String component, Object value, long timestamp) {
        this.project = project;
        this.component = component;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}