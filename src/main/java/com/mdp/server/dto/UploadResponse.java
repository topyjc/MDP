package com.mdp.server.dto;

public class UploadResponse {

    private boolean success;
    private String message;
    private String group;
    private String fileName;
    private String fileUrl;

    public UploadResponse() {}

    public UploadResponse(boolean success, String message, String group, String fileName, String fileUrl) {
        this.success = success;
        this.message = message;
        this.group = group;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getGroup() {
        return group;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}