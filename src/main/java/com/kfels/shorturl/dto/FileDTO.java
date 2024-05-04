package com.kfels.shorturl.dto;

public class FileDTO {
    private String name;
    private String message;
    private String url;
    private String deleteUrl;

    public String getDeleteUrl() {
        return this.deleteUrl;
    }

    public void setDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    public FileDTO() {
    }

    public FileDTO(String name, String message, String url, String deleteUrl) {
        this.name = name;
        this.message = message;
        this.url = url;
        this.deleteUrl = deleteUrl;
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

}
