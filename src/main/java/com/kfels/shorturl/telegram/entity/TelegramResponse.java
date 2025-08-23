package com.kfels.shorturl.telegram.entity;

import lombok.Data;

@Data
public class TelegramResponse {
    private boolean ok;
    private TelegramFile result;

    @Override
    public String toString() {
        return String.format("{ ok='%s', result='%s'}", isOk(), getResult());
    }

}
