package com.kfels.shorturl.dto;

import lombok.Data;

@Data
public class RequestDTO {
    private String surl;
    private String url;

    public RequestDTO() {
        super();
    }
}
