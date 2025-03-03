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
import java.util.Iterator;
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

    private static final Path storagePath = Paths.get("./data/uploads");
    private static final Logger LOG = Logger.getLogger(UploadedFileServiceImpl.class.getName());

    @Override
    public FileDTO save(MultipartFile file, String creatorIp, int expiry) {
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
                LOG.warning(String.format("File size of [%f MB] is greater then max size of [%f MB]", size, maxSize));
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

            if (expiry < 0 || expiry > 5259600) {
                expiry = 43200;
            }
            LocalDateTime expiryTime = LocalDateTime.now().plus(expiry, ChronoUnit.MINUTES);
            UploadedFile uploadedFile = new UploadedFile(name, creatorIp, mimeType, expiryTime);
            String fileName = uploadedFile.getFileName();

            LOG.info(String.format("Uploaded file: %s", name));
            LOG.info(String.format("Saved as: %s", fileName));
            Files.copy(file.getInputStream(), storagePath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            fileRepo.save(uploadedFile);

            String url = String.format("/file/%s/%s", uploadedFile.getDownloadKey(), name);
            String deleteUrl = String.format("/deleteFile/%s/%s", uploadedFile.getDownloadKey(),
                    uploadedFile.getDeleteKey());
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
                    LOG.warning(
                            String.format("File size of [%f MB] is greater then max size of [%f MB]", size, maxSize));
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

                LOG.info(String.format("Uploaded file: %s", name));
                LOG.info(String.format("Saved as: %s", fileName));
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
                LOG.warning(String.format("Could not read the file: %s", filePath.toString()));
                return null;
            }
            Resource resource = new UrlResource(uri);
            if (resource.exists() && resource.isReadable()) {
                fileRepo.save(file);
                return new FileLoadDTO(file, resource);
            } else {
                LOG.warning(String.format("Could not read the file: %s", file.toString()));
                return null;
            }
        } catch (Exception e) {
            LOG.warning(String.format("Could not retrieve the file with key: %s", downloadKey));
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
                LOG.warning(String.format("Could not delete file with download key [%s] and deleteKey [%s]",
                        downloadKey, deleteKey));
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
                LOG.warning(String.format("UploadedFile with id [%d] is not active.", uploadedFile.getId()));
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
                fileDto.setName(CommonUtils.urlDecode(file.getName()));
                fileDto.setMimeType(file.getMimeType());
                fileDto.setCreatorIp(file.getCreatorIp());
                fileDto.setCreated(file.getCreated());
                fileDto.setExpiryTime(file.getExpiryTime());
                fileDto.setDownloadKey(file.getDownloadKey());
                fileDto.setHits(file.getHits());

                String filename = file.getFileName();
                String url = String.format("/file/%s/%s", file.getDownloadKey(), file.getName());
                String deleteUrl = String.format("/deleteFile/%s/%s", file.getDownloadKey(), file.getDeleteKey());

                fileDto.setFileName(filename);
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
                    LOG.warning(String.format("Could not retrieve file details for: %s", file.getFileName()));
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

        if (fileList == null || fileList.size() == 0)
            return new ResponseDTO("OK");

        int count = 0;
        Iterator<UploadedFile> fileIterator = fileList.iterator();
        while (fileIterator.hasNext()) {
            UploadedFile file = fileIterator.next();
            if (now.isAfter(file.getExpiryTime())) {
                fileIterator.remove();
                delete(file.getDownloadKey(), file.getDeleteKey());
                count++;
            }
        }

        String msg = String.format("Cron ran at: %s and removed %d objects.", now.toString(), count);
        LOG.info(msg);
        return new ResponseDTO("OK", null, msg);
    }
}
