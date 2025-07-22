package com.kfels.shorturl.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FileDetailsDTO {
    private String name;
    private String size;
    private String modified;
    private String url;
    private String mimeType;
    private String creatorIp;
    private String fileName;
    private String deleteUrl;
    private LocalDateTime created;
    private LocalDateTime expiryTime;
    private Boolean active;
    private String downloadKey;
    private int hits;

    public FileDetailsDTO() {
    }
}
