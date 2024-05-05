package com.kfels.shorturl.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.kfels.shorturl.utils.CommonUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

@Entity
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String mimeType;
    private String fileName;
    private String downloadKey;
    private String downloadKeyHash;
    private String deleteKey;
    private LocalDateTime created;
    private LocalDateTime expiryTime;
    private Boolean active;
    private String creatorIp;
    private int hits;
    private LocalDateTime lastHit;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id", nullable = true)
    private List<Datalog> logs;

    public UploadedFile() {
    }

    public UploadedFile(String name, String creatorIp, String mimeType, LocalDateTime expiryTime) {
        String downloadKey = CommonUtils.generateStringForFileurl();
        this.name = name;
        this.mimeType = mimeType;
        this.fileName = UUID.randomUUID().toString();
        this.downloadKey = downloadKey;
        this.downloadKeyHash = CommonUtils.getHash(downloadKey);
        this.deleteKey = CommonUtils.randString(32, 3);
        this.created = LocalDateTime.now();
        this.expiryTime = expiryTime;
        this.active = true;
        this.creatorIp = creatorIp;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadKey() {
        return this.downloadKey;
    }

    public void setDownloadKey(String downloadKey) {
        this.downloadKey = downloadKey;
    }

    public String getDownloadKeyHash() {
        return this.downloadKeyHash;
    }

    public void setDownloadKeyHash(String downloadKeyHash) {
        this.downloadKeyHash = downloadKeyHash;
    }

    public String getDeleteKey() {
        return this.deleteKey;
    }

    public void setDeleteKey(String deleteKey) {
        this.deleteKey = deleteKey;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getExpiryTime() {
        return this.expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Boolean isActive() {
        return this.active;
    }

    public Boolean getActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCreatorIp() {
        return this.creatorIp;
    }

    public void setCreatorIp(String creatorIp) {
        this.creatorIp = creatorIp;
    }

    public int getHits() {
        return this.hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public LocalDateTime getLastHit() {
        return this.lastHit;
    }

    public void setLastHit(LocalDateTime lastHit) {
        this.lastHit = lastHit;
    }

    public List<Datalog> getLogs() {
        return this.logs;
    }

    public void setLogs(List<Datalog> logs) {
        this.logs = logs;
    }

    // Custom function to update variables when a short url is accessed
    public String accessFile(String ip, String browserHeaders) {
        if (active) {
            Datalog datalog = new Datalog(ip, browserHeaders);
            this.logs.add(datalog);
            this.hits = this.hits + 1;
            this.lastHit = LocalDateTime.now();
            return this.fileName;
        } else {
            return null;
        }
    }

}
