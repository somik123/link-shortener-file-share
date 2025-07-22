package com.kfels.shorturl.dto;

import lombok.Data;

@Data
public class ShorturlDTO {
    private String surl;
    private String longUrl;
    private String deleteKey;
    private boolean isEnabled;

    public ShorturlDTO() {
    }

    public ShorturlDTO(String surl, boolean isEnabled) {
        this(surl, "", "", isEnabled);
    }

    public ShorturlDTO(String surl, String longUrl, String deleteKey, boolean isEnabled) {
        this.surl = surl;
        this.longUrl = longUrl;
        this.deleteKey = deleteKey;
        this.isEnabled = isEnabled;
    }
}
