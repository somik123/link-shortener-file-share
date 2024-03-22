package com.kfels.shorturl.dto;

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

    public String getSurl() {
        return this.surl;
    }

    public void setSurl(String surl) {
        this.surl = surl;
    }

    public String getLongUrl() {
        return this.longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getDeleteKey() {
        return this.deleteKey;
    }

    public void setDeleteKey(String deleteKey) {
        this.deleteKey = deleteKey;
    }

    public boolean isIsEnabled() {
        return this.isEnabled;
    }

    public boolean getIsEnabled() {
        return this.isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

}
