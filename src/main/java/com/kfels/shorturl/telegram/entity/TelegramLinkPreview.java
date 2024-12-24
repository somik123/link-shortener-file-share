package com.kfels.shorturl.telegram.entity;

public class TelegramLinkPreview {
    private String url;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return String.format("{ url='%s'}", getUrl());
    }
}
