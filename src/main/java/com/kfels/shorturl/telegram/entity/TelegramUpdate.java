package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
public class TelegramUpdate {
    private String update_id;
    private TelegramMessage message;
    private TelegramMessage edited_message;

    public TelegramUpdate() {
    }

    @Override
    public String toString() {
        return String.format("{ update_id='%s', message='%s', edited_message='%s'}", getUpdate_id(), getMessage(),
                getEdited_message());
    }
}
