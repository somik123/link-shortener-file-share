package com.kfels.shorturl.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kfels.shorturl.dto.FileDTO;
import com.kfels.shorturl.dto.FileLoadDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping(value = "/file", produces = MediaType.APPLICATION_JSON_VALUE)

public class FileController {

    @Autowired
    UploadedFileService storageService;

    private static Logger LOG = Logger.getLogger(FileController.class.getName());

    @PostMapping(value = "/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("expiry_hour") int expiryHours, HttpServletRequest request) {
        String creatorIp = CommonUtils.getClientIpAddress(request);
        FileDTO fileDTO = storageService.save(file, creatorIp, expiryHours);
        if (fileDTO == null) {
            String message = "Upload failed.";
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseDTO("FAIL", null, message));
        } else {

            // Notify admin
            String url = System.getenv("SITE_FULL_URL") + fileDTO.getUrl().substring(1);
            String deleteUrl = "/deleteFile_" + fileDTO.getDownloadKey() + "_"
                    + fileDTO.getDeleteKey();
            String msg = "New File uploaded: " + url + "\n"
                    + "Name: " + file.getOriginalFilename() + "\n"
                    + "Size: " + CommonUtils.formatSize(file.getSize()) + "\n"
                    + "Type: " + file.getContentType() + "\n"
                    + "Delete: " + deleteUrl;
            CommonUtils.asynSendTelegramMessage(msg);

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseDTO("OK", fileDTO, null));
        }
    }

    // Allow to download with just downloadKey or downloadKey with filename
    @GetMapping("/{downloadKey}/{fileName}")
    public ResponseEntity<?> downloadFile(@PathVariable String downloadKey,
            @PathParam("fileName") String fileName, HttpServletRequest request) {
        LOG.info("Download key: " + downloadKey);
        return downloadFile(downloadKey, request);
    }

    @GetMapping("/{downloadKey}")
    public ResponseEntity<?> downloadFile(@PathVariable String downloadKey, HttpServletRequest request) {
        String message;
        try {
            String creatorIp = CommonUtils.getClientIpAddress(request);
            String browserHeaders = request.getHeader("User-Agent");
            FileLoadDTO fileLoadDTO = storageService.load(downloadKey, creatorIp, browserHeaders);
            Resource resource = fileLoadDTO.getResource();
            UploadedFile file = fileLoadDTO.getFile();
            if (resource == null) {
                message = "file does not exist.";
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ResponseDTO("FAIL", null, message));
            }
            String mimeType = storageService.getMimeType(downloadKey);
            if (mimeType == null || mimeType.length() < 3)
                mimeType = "application/octet-stream";
            MediaType mediaType = MediaType.parseMediaType(mimeType);

            InputStream in = resource.getInputStream();

            LOG.info(resource.toString());
            LOG.info(mediaType.toString());

            return ResponseEntity.ok().contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(new InputStreamResource(in));

        } catch (IOException e) {
            message = "Error while processing file.";
            LOG.warning(message);
            CommonUtils.logErrors(LOG, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDTO("FAIL", null, message));
        } catch (Exception e) {
            message = "Error while processing file.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("FAIL", null, message));
        }
    }

    @GetMapping("/cron")
    public ResponseDTO runCronJobs() {
        return storageService.cronJobs();
    }

}
