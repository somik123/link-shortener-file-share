package com.kfels.shorturl.dto;

import lombok.Data;

@Data
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
}
