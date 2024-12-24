package com.kfels.shorturl.telegram.entity;

import java.util.List;

public class TelegramMessage {
    private int message_id;
    private TelegramChat from;
    private TelegramChat chat;
    private int date;
    private int edit_date;
    private String media_group_id;
    private String text;
    private List<TelegramEntities> entities;
    private List<TelegramFile> photo;
    private TelegramFile document;
    private TelegramFile audio;
    private TelegramFile video;
    private TelegramLinkPreview link_preview_options;

    public TelegramMessage() {
    }

    public int getMessage_id() {
        return this.message_id;
    }

    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public TelegramChat getFrom() {
        return this.from;
    }

    public void setFrom(TelegramChat from) {
        this.from = from;
    }

    public TelegramChat getChat() {
        return this.chat;
    }

    public void setChat(TelegramChat chat) {
        this.chat = chat;
    }

    public int getDate() {
        return this.date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getEdit_date() {
        return this.edit_date;
    }

    public void setEdit_date(int edit_date) {
        this.edit_date = edit_date;
    }

    public String getMedia_group_id() {
        return this.media_group_id;
    }

    public void setMedia_group_id(String media_group_id) {
        this.media_group_id = media_group_id;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TelegramEntities> getEntities() {
        return this.entities;
    }

    public void setEntities(List<TelegramEntities> entities) {
        this.entities = entities;
    }

    public List<TelegramFile> getPhoto() {
        return this.photo;
    }

    public void setPhoto(List<TelegramFile> photo) {
        this.photo = photo;
    }

    public TelegramFile getDocument() {
        return this.document;
    }

    public void setDocument(TelegramFile document) {
        this.document = document;
    }

    public TelegramFile getAudio() {
        return this.audio;
    }

    public void setAudio(TelegramFile audio) {
        this.audio = audio;
    }

    public TelegramFile getVideo() {
        return this.video;
    }

    public void setVideo(TelegramFile video) {
        this.video = video;
    }

    public TelegramLinkPreview getLink_preview_options() {
        return this.link_preview_options;
    }

    public void setLink_preview_options(TelegramLinkPreview link_preview_options) {
        this.link_preview_options = link_preview_options;
    }

    @Override
    public String toString() {
        return String.format(
                "{ message_id='%d', from='%s', chat='%s', date='%d', edit_date='%d', media_group_id='%s', text='%s', entities='%s', photo='%s', document='%s', audio='%s', video='%s', link_preview_options='%s'}",
                getMessage_id(), getFrom(), getChat(), getDate(), getEdit_date(), getMedia_group_id(), getText(),
                getEntities(), getPhoto(), getDocument(), getAudio(), getVideo(), getLink_preview_options());
    }

}
