package com.kfels.shorturl.telegram.entity;

public class TelegramEntities {
    private int offset;
    private int length;
    private String type;

    public TelegramEntities() {
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "{" +
            " offset='" + getOffset() + "'" +
            ", length='" + getLength() + "'" +
            ", type='" + getType() + "'" +
            "}";
    }
}