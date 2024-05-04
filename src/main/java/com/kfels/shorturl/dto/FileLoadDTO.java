package com.kfels.shorturl.dto;

import com.kfels.shorturl.entity.UploadedFile;
import org.springframework.core.io.Resource;

public class FileLoadDTO {
    UploadedFile file;
    Resource resource;

    public FileLoadDTO() {
    }

    public FileLoadDTO(UploadedFile file, Resource resource) {
        this.file = file;
        this.resource = resource;
    }

    public UploadedFile getFile() {
        return this.file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public Resource getResource() {
        return this.resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

}
