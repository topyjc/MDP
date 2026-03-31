package com.mdp.server.dto;

public class MediaUploadResponse {

    private String fileName;
    private String fileUrl;
    private String message;

    public MediaUploadResponse() {
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}