package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
public class TelegramChat {
    private int id;
    private boolean is_bot;
    private String first_name;
    private String last_name;
    private String username;
    private String language_code;
    private String type;

    public TelegramChat() {
    }

    public boolean isIs_bot() {
        return is_bot;
    }

    public void setIs_bot(boolean is_bot) {
        this.is_bot = is_bot;
    }

    @Override
    public String toString() {
        return String.format(
                "{ id='%d', is_bot='%s', first_name='%s', last_name='%s', username='%s', language_code='%s', type='%s'}",
                getId(), isIs_bot(), getFirst_name(), getLast_name(), getUsername(), getLanguage_code(), getType());
    }

}
