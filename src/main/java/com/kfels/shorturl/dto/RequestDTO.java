package com.kfels.shorturl.dto;

public class RequestDTO {
    private String surl;
    private String url;

    public RequestDTO() {
        super();
    }

    public String getSurl() {
        return this.surl;
    }

    public void setSurl(String surl) {
        this.surl = surl;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
