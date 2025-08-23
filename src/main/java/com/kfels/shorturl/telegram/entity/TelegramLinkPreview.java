package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
public class TelegramLinkPreview {
    private String url;

    @Override
    public String toString() {
        return String.format("{ url='%s'}", getUrl());
    }
}
