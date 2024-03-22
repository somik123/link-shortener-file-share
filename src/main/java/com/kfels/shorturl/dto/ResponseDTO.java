package com.kfels.shorturl.dto;

public class ResponseDTO {
    private String status;
    private Object content;
    private String error;

    public ResponseDTO(String status, Object content, String error) {
        super();
        this.status = status;
        this.content = content;
        this.error = error;
    }

    public ResponseDTO(String status, Object content) {
        this(status, content, "");
    }

    public ResponseDTO(String status) {
        this(status, "", "");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
