package com.kfels.shorturl.telegram.entity;

public class TelegramUpdate {
    private String update_id;
    private TelegramMessage message;

    public TelegramUpdate() {
    }

    public String getUpdate_id() {
        return this.update_id;
    }

    public void setUpdate_id(String update_id) {
        this.update_id = update_id;
    }

    public TelegramMessage getMessage() {
        return this.message;
    }

    public void setMessage(TelegramMessage message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "{" +
                " update_id='" + getUpdate_id() + "'" +
                ", message='" + getMessage() + "'" +
                "}";
    }
}
