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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

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

    private static final Logger LOG = Logger.getLogger(FileController.class.getName());

    @PostMapping(value = "/upload")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("file_expiry") int expiry, HttpServletRequest request) {
        String creatorIp = CommonUtils.getClientIpAddress(request);
        FileDTO fileDTO = storageService.save(file, creatorIp, expiry);
        if (fileDTO == null) {
            String message = "Upload failed.";
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseDTO("FAIL", null, message));
        } else {
            // Notify admin
            String url = String.format("%s%s", System.getenv("SITE_FULL_URL"), fileDTO.getDownloadKey());
            String deleteUrl = String.format("/deleteFile_%s_%s", fileDTO.getDownloadKey(), fileDTO.getDeleteKey());
            String msg = String.format("New File uploaded: %s\nName: %s\nSize: %s\nType: %s\nDelete: %s", url,
                    file.getOriginalFilename(), CommonUtils.formatSize(file.getSize()), file.getContentType(),
                    deleteUrl);
            CommonUtils.asynSendTelegramMessage(msg);

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseDTO("OK", fileDTO, null));
        }
    }

    // Allow to download with just downloadKey or downloadKey with filename
    @GetMapping("/{downloadKey}/{fileName}")
    public ResponseEntity<?> downloadFile(@PathVariable String downloadKey,
            @PathParam("fileName") String fileName, HttpServletRequest request) {
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
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            String.format("inline; filename=\"%s\"", file.getName()))
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

    @GetMapping("/{downloadKey}")
    public RedirectView downloadFile(@PathVariable String downloadKey, HttpServletRequest request) {
        LOG.info(String.format("Download key: %s", downloadKey));

        UploadedFile file = storageService.getUploadFileFromDownloadKey(downloadKey);
        if (file == null) {
            return new RedirectView("/");
        } else {
            LOG.info(String.format("File name: %s", file.getName()));
            String url = String.format("%s/%s", downloadKey, file.getName());
            return new RedirectView(url);
        }
    }

    @GetMapping("/cron")
    public ResponseDTO runCronJobs() {
        return storageService.cronJobs();
    }

}
