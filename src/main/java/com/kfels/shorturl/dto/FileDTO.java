package com.kfels.shorturl.dto;

import lombok.Data;

@Data
public class FileDTO {
    private String name;
    private String message;
    private String url;
    private String deleteUrl;
    private String downloadKey;
    private String deleteKey;
    private boolean isEnabled;

    public FileDTO() {
    }

    public FileDTO(String name, String message, String url, String deleteUrl, String downloadKey, String deleteKey,
            boolean isEnabled) {
        this.name = name;
        this.message = message;
        this.url = url;
        this.deleteUrl = deleteUrl;
        this.downloadKey = downloadKey;
        this.deleteKey = deleteKey;
        this.isEnabled = isEnabled;
    }
}
