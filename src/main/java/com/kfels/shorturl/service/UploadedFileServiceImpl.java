package com.kfels.shorturl.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kfels.shorturl.dto.FileDTO;
import com.kfels.shorturl.dto.FileDetailsDTO;
import com.kfels.shorturl.dto.FileLoadDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.repo.UploadedFileRepo;
import com.kfels.shorturl.utils.CommonUtils;

@Service
public class UploadedFileServiceImpl implements UploadedFileService {

    @Autowired
    UploadedFileRepo fileRepo;

    private final Path storagePath = Paths.get("uploads/");

    private static Logger LOG = Logger.getLogger(UploadedFileServiceImpl.class.getName());

    @Override
    public FileDTO save(MultipartFile file, String creatorIp, int expiryHours) {
        try {
            String name = file.getOriginalFilename();
            String mimeType = file.getContentType();
            float size = file.getSize() / (1024 * 1024);
            float maxSize = 0;
            String maxSizeStr = System.getenv("UPLOADFILE_MAX_SIZE");
            if (maxSizeStr != null && maxSizeStr.length() > 0) {
                maxSize = Float.parseFloat(maxSizeStr);
            }
            if (maxSize > 0 && size > maxSize) {
                LOG.warning("File size of [" + size + " MB] is greater then max size of [" + maxSize + " MB]");
                return null;
            }

            name = CommonUtils.cleanFileName(name);
            name = URLEncoder.encode(name, StandardCharsets.UTF_8);
            if (name == null || name.length() < 2) {
                LOG.warning("Missing filename.");
                return null;
            } else if (name.contains("..")) {
                LOG.warning("Invalid filename.");
                return null;
            }

            LocalDateTime expiryTime = LocalDateTime.now().plus(expiryHours, ChronoUnit.HOURS);
            UploadedFile uploadedFile = new UploadedFile(name, creatorIp, mimeType, expiryTime);
            String fileName = uploadedFile.getFileName();

            LOG.info("Uploaded file: " + name);
            LOG.info("Saved as: " + fileName);
            Files.copy(file.getInputStream(), storagePath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            fileRepo.save(uploadedFile);

            String url = "/file/" + uploadedFile.getDownloadKey() + "/" + name;
            String deleteUrl = "/deleteFile/" + uploadedFile.getDownloadKey() + "/" + uploadedFile.getDeleteKey();
            return new FileDTO(name, "File uploaded successfully.", url, deleteUrl,
                    uploadedFile.getDownloadKey(), uploadedFile.getDeleteKey());
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
            return null;
        }
    }

    @Override
    public UploadedFile saveFromTelegram(String filePath, int expiryHours) {
        try {
            Path file = Paths.get(filePath);
            if (Files.exists(file) && Files.isWritable(file)) {
                String mimeType = Files.probeContentType(file);
                float size = Files.size(file) / (1024 * 1024);
                float maxSize = 0;
                String maxSizeStr = System.getenv("UPLOADFILE_MAX_SIZE");
                if (maxSizeStr != null && maxSizeStr.length() > 0) {
                    maxSize = Float.parseFloat(maxSizeStr);
                }
                if (maxSize > 0 && size > maxSize) {
                    LOG.warning("File size of [" + size + " MB] is greater then max size of [" + maxSize + " MB]");
                    return null;
                }
                int i = filePath.lastIndexOf("/");
                String name = filePath.substring(i + 1);

                if (name == null || name.length() < 2) {
                    LOG.warning("Missing filename.");
                    return null;
                } else if (name.contains("..")) {
                    LOG.warning("Invalid filename.");
                    return null;
                }

                LocalDateTime expiryTime = LocalDateTime.now().plus(expiryHours, ChronoUnit.HOURS);
                UploadedFile uploadedFile = new UploadedFile(name, "Telegram", mimeType, expiryTime);
                String fileName = uploadedFile.getFileName();

                LOG.info("Uploaded file: " + name);
                LOG.info("Saved as: " + fileName);
                Files.copy(Files.newInputStream(file), storagePath.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING);
                fileRepo.save(uploadedFile);
                // Remove temp file
                Files.delete(file);

                return uploadedFile;
            }
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
        }
        return null;
    }

    @Override
    public FileLoadDTO load(String downloadKey, String ip, String browserHeaders) {
        try {
            UploadedFile file = getUploadFileFromDownloadKey(downloadKey);
            if (file == null)
                return null;
            Path filePath = storagePath.resolve(file.accessFile(ip, browserHeaders));
            URI uri = filePath.toUri();
            if (uri == null) {
                LOG.warning("Could not read the file: " + filePath.toString());
                return null;
            }
            Resource resource = new UrlResource(uri);
            if (resource.exists() && resource.isReadable()) {
                fileRepo.save(file);
                return new FileLoadDTO(file, resource);
            } else {
                LOG.warning("Could not read the file: " + file.toString());
                return null;
            }
        } catch (Exception e) {
            LOG.warning("Could not retrieve the file with key: " + downloadKey);
            CommonUtils.logErrors(LOG, e);
            return null;
        }
    }

    @Override
    public boolean delete(String downloadKey, String deleteKey) {
        Boolean status = false;
        UploadedFile uploadedFile = getUploadFileFromDownloadKey(downloadKey);
        if (uploadedFile == null)
            return false;

        if (uploadedFile.getDeleteKey().equals(deleteKey)) {
            try {
                Path file = storagePath.resolve(uploadedFile.getFileName());
                if (Files.exists(file) && Files.isWritable(file)) {
                    Files.delete(file);
                    if (!Files.exists(file))
                        status = true;
                }
            } catch (IOException e) {
                LOG.warning("Could not delete file with download key [" + downloadKey
                        + "] and deleteKey [" + deleteKey + "]");
                CommonUtils.logErrors(LOG, e);
            }
            fileRepo.delete(uploadedFile);
        }

        return status;
    }

    @Override
    public String getMimeType(String downloadKey) {
        UploadedFile uploadedFile = getUploadFileFromDownloadKey(downloadKey);
        if (uploadedFile == null)
            return null;
        String mimeType = uploadedFile.getMimeType();
        return mimeType;
    }

    @Override
    public UploadedFile getUploadFileFromDownloadKey(String downloadKey) {
        if (downloadKey == null || downloadKey.length() < 3) {
            LOG.warning("Missing download key.");
            return null;
        }
        String downloadKeyHash = CommonUtils.getHash(downloadKey);
        List<UploadedFile> fileList = fileRepo.findByDownloadKeyHash(downloadKeyHash);
        if (fileList != null && fileList.size() > 0) {
            UploadedFile uploadedFile = fileList.get(0);
            if (uploadedFile.isActive())
                return uploadedFile;
            else {
                LOG.warning("UploadedFile with id [" + uploadedFile.getId() + "] is not active.");
                return null;
            }
        } else
            return null;
    }

    @Override
    public List<FileDetailsDTO> getAllFileDetails() {
        List<UploadedFile> fileList = fileRepo.findAllCustom();
        List<FileDetailsDTO> fileDtoList = new ArrayList<>();

        if (fileList != null && fileList.size() > 0) {
            for (UploadedFile file : fileList) {
                FileDetailsDTO fileDto = new FileDetailsDTO();
                fileDto.setName(file.getName());
                fileDto.setMimeType(file.getMimeType());
                fileDto.setCreatorIp(file.getCreatorIp());
                fileDto.setCreated(file.getCreated());
                fileDto.setExpiryTime(file.getExpiryTime());
                fileDto.setDownloadKey(file.getDownloadKey());
                fileDto.setHits(file.getHits());

                String filename = file.getFileName();
                String url = "/file/" + file.getDownloadKey() + "/" + file.getName();
                String deleteUrl = "/deleteFile/" + file.getDownloadKey() + "/" + file.getDeleteKey();

                fileDto.setFileName(filename.substring(0, 18) + " " + filename.substring(18));
                fileDto.setUrl(url);
                fileDto.setDeleteUrl(deleteUrl);

                try {
                    Path filePath = storagePath.resolve(file.getFileName());
                    if (Files.exists(filePath) && Files.isWritable(filePath)) {
                        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                        fileDto.setSize(CommonUtils.formatSize(attr.size()));
                        fileDto.setModified(attr.lastModifiedTime().toString());
                    }
                } catch (IOException e) {
                    LOG.warning("Could not retrieve file details for: " + file.getFileName());
                    CommonUtils.logErrors(LOG, e);
                    return null;
                }
                fileDtoList.add(fileDto);
            }
        } else {
            LOG.info("Uploaded file list is empty for admin view.");
        }
        return fileDtoList;
    }

    @Override
    public ResponseDTO cronJobs() {
        List<UploadedFile> fileList = fileRepo.findAllCustom();
        LocalDateTime now = LocalDateTime.now();
        List<ResponseDTO> responseList = new ArrayList<>();

        if (fileList != null && fileList.size() > 0) {
            for (UploadedFile file : fileList) {
                if (now.isAfter(file.getExpiryTime())) {
                    String message = "Deleted: " + file.getName() + " [" + file.getFileName() + "]";
                    String status = delete(file.getDownloadKey(), file.getDeleteKey()) ? "OK" : "FAIL";
                    ResponseDTO response = new ResponseDTO(status, message);
                    responseList.add(response);
                }
            }
        }
        return new ResponseDTO("OK", responseList, "Cron ran at:" + now.toString());
    }
}
