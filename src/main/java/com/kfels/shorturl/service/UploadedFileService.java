package com.kfels.shorturl.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.kfels.shorturl.dto.FileDTO;
import com.kfels.shorturl.dto.FileDetailsDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.entity.UploadedFile;

public interface UploadedFileService {
    FileDTO save(MultipartFile file, String creatorIp, int expiryHours);

    Resource load(String downloadKey, String ip, String browserHeaders);

    boolean delete(String downloadKey, String deleteKey);

    String getMimeType(String downloadKey);

    UploadedFile getUploadFileFromDownloadKey(String downloadKey);

    List<FileDetailsDTO> getAllFileDetails();

    ResponseDTO cronJobs();
}
