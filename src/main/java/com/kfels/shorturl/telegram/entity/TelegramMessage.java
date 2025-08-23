package com.kfels.shorturl.telegram.entity;

import java.util.List;

import lombok.Data;

@Data
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

    @Override
    public String toString() {
        return String.format(
                "{ message_id='%d', from='%s', chat='%s', date='%d', edit_date='%d', media_group_id='%s', text='%s', entities='%s', photo='%s', document='%s', audio='%s', video='%s', link_preview_options='%s'}",
                getMessage_id(), getFrom(), getChat(), getDate(), getEdit_date(), getMedia_group_id(), getText(),
                getEntities(), getPhoto(), getDocument(), getAudio(), getVideo(), getLink_preview_options());
    }

}
