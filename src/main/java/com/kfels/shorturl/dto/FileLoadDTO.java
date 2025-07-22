package com.kfels.shorturl.dto;

import com.kfels.shorturl.entity.UploadedFile;

import lombok.Data;

import org.springframework.core.io.Resource;

@Data
public class FileLoadDTO {
    UploadedFile file;
    Resource resource;

    public FileLoadDTO() {
    }

    public FileLoadDTO(UploadedFile file, Resource resource) {
        this.file = file;
        this.resource = resource;
    }
}
