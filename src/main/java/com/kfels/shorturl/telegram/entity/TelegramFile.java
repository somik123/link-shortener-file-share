package com.kfels.shorturl.telegram.entity;

public class TelegramFile {
    private String file_name;
    private String mime_type;
    private String file_id;
    private String file_unique_id;
    private int file_size;
    private String file_path;
    private int duration;
    private int width;
    private int height;
    private TelegramFile thumbnail;
    private TelegramFile thumb;

    public TelegramFile() {
    }

    public String getFile_name() {
        return this.file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getMime_type() {
        return this.mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public String getFile_id() {
        return this.file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
    }

    public String getFile_unique_id() {
        return this.file_unique_id;
    }

    public void setFile_unique_id(String file_unique_id) {
        this.file_unique_id = file_unique_id;
    }

    public int getFile_size() {
        return this.file_size;
    }

    public void setFile_size(int file_size) {
        this.file_size = file_size;
    }

    public String getFile_path() {
        return this.file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public TelegramFile getThumbnail() {
        return this.thumbnail;
    }

    public void setThumbnail(TelegramFile thumbnail) {
        this.thumbnail = thumbnail;
    }

    public TelegramFile getThumb() {
        return this.thumb;
    }

    public void setThumb(TelegramFile thumb) {
        this.thumb = thumb;
    }

    @Override
    public String toString() {
        return String.format(
                "{ file_name='%s', mime_type='%s', file_id='%s', file_unique_id='%s', file_size='%d', file_path='%s', duration='%d', width='%d', height='%d', thumbnail='%s', thumb='%s'}",
                getFile_name(), getMime_type(), getFile_id(), getFile_unique_id(), getFile_size(), getFile_path(),
                getDuration(), getWidth(), getHeight(), getThumbnail(), getThumb());
    }

}
