package com.kfels.shorturl.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.kfels.shorturl.entity.UploadedFile;

public interface UploadedFileRepo extends CrudRepository<UploadedFile, Integer> {
    List<UploadedFile> findByDownloadKeyHash(String downloadKeyHash);

    List<UploadedFile> findByExpiryTime(LocalDateTime expiryTime);

    @Query("SELECT f FROM UploadedFile f")
    List<UploadedFile> findAllCustom();
}
