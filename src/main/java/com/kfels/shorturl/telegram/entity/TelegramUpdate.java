package com.kfels.shorturl.telegram.entity;

public class TelegramUpdate {
    private String update_id;
    private TelegramMessage message;
    private TelegramMessage edited_message;

    public TelegramUpdate() {
    }

    public String getUpdate_id() {
        return this.update_id;
    }

    public void setUpdate_id(String update_id) {
        this.update_id = update_id;
    }

    public TelegramMessage getMessage() {
        return this.message != null ? this.message : this.edited_message;
    }

    public void setMessage(TelegramMessage message) {
        this.message = message;
    }

    public TelegramMessage getEdited_message() {
        return this.edited_message;
    }

    public void setEdited_message(TelegramMessage edited_message) {
        this.edited_message = edited_message;
    }

    @Override
    public String toString() {
        return String.format("{ update_id='%s', message='%s', edited_message='%s'}", getUpdate_id(), getMessage(),
                getEdited_message());
    }
}
