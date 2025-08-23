package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
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

    @Override
    public String toString() {
        return String.format(
                "{ file_name='%s', mime_type='%s', file_id='%s', file_unique_id='%s', file_size='%d', file_path='%s', duration='%d', width='%d', height='%d', thumbnail='%s', thumb='%s'}",
                getFile_name(), getMime_type(), getFile_id(), getFile_unique_id(), getFile_size(), getFile_path(),
                getDuration(), getWidth(), getHeight(), getThumbnail(), getThumb());
    }

}
