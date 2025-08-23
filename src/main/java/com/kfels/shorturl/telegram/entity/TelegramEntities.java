package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
public class TelegramEntities {
    private int offset;
    private int length;
    private String type;

    public TelegramEntities() {
    }

    @Override
    public String toString() {
        return String.format("{ offset='%d', length='%d', type='%s'}", getOffset(), getLength(), getType());
    }
}