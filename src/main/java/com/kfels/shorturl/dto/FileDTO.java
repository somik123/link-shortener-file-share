package com.kfels.shorturl.dto;

public class FileDTO {
    private String name;
    private String message;
    private String url;
    private String deleteUrl;
    private String downloadKey;
    private String deleteKey;

    public FileDTO() {
    }

    public FileDTO(String name, String message, String url, String deleteUrl, String downloadKey, String deleteKey) {
        this.name = name;
        this.message = message;
        this.url = url;
        this.deleteUrl = deleteUrl;
        this.downloadKey = downloadKey;
        this.deleteKey = deleteKey;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDeleteUrl() {
        return this.deleteUrl;
    }

    public void setDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    public String getDownloadKey() {
        return this.downloadKey;
    }

    public void setDownloadKey(String downloadKey) {
        this.downloadKey = downloadKey;
    }

    public String getDeleteKey() {
        return this.deleteKey;
    }

    public void setDeleteKey(String deleteKey) {
        this.deleteKey = deleteKey;
    }
}
