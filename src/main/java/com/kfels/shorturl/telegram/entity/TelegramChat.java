package com.kfels.shorturl.telegram.entity;

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

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isIs_bot() {
        return this.is_bot;
    }

    public boolean getIs_bot() {
        return this.is_bot;
    }

    public void setIs_bot(boolean is_bot) {
        this.is_bot = is_bot;
    }

    public String getFirst_name() {
        return this.first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return this.last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLanguage_code() {
        return this.language_code;
    }

    public void setLanguage_code(String language_code) {
        this.language_code = language_code;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format(
                "{ id='%d', is_bot='%s', first_name='%s', last_name='%s', username='%s', language_code='%s', type='%s'}",
                getId(), isIs_bot(), getFirst_name(), getLast_name(), getUsername(), getLanguage_code(), getType());
    }

}
