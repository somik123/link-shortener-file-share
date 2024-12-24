package com.kfels.shorturl.telegram.entity;

public class TelegramResponse {
    private boolean ok;
    private TelegramFile result;

    public boolean isOk() {
        return this.ok;
    }

    public boolean getOk() {
        return this.ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public TelegramFile getResult() {
        return this.result;
    }

    public void setResult(TelegramFile result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return String.format("{ ok='%s', result='%s'}", isOk(), getResult());
    }

}
