package com.kfels.shorturl.entity;

import com.kfels.shorturl.utils.CommonUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

@Entity
public class Shorturl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String surl;
    private String surlHash;

    @Nonnull
    @Column(length = 2000)
    private String longUrl;
    private String longUrlHash;

    @Nonnull
    private String deleteKey;

    private String creatorIp;

    private int hits;
    private LocalDateTime created;
    private LocalDateTime lastHit;
    private boolean isEnabled;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "surl_id", nullable = true)
    private List<Datalog> logs;

    public Shorturl() {
        super();
    }

    public Shorturl(String longUrl, String creatorIp, String surl) {
        // Generate short url is not provided
        if (surl == null || surl.length() == 0)
            surl = CommonUtils.randString();

        this.surl = surl;
        this.surlHash = CommonUtils.getHash(surl);
        this.longUrl = longUrl;
        this.longUrlHash = CommonUtils.getHash(longUrl);
        this.deleteKey = CommonUtils.randString(32);
        this.creatorIp = creatorIp;
        this.hits = 0;
        this.created = LocalDateTime.now();
        this.lastHit = null;
        this.isEnabled = true;
        this.logs = new ArrayList<>();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSurl() {
        return this.surl;
    }

    public void setSurl(String surl) {
        this.surl = surl;
    }

    public String getLongUrl() {
        return this.longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getDeleteKey() {
        return this.deleteKey;
    }

    public void setDeleteKey(String deleteKey) {
        this.deleteKey = deleteKey;
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

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastHit() {
        return this.lastHit;
    }

    public void setLastHit(LocalDateTime lastHit) {
        this.lastHit = lastHit;
    }

    public boolean isIsEnabled() {
        return this.isEnabled;
    }

    public boolean getIsEnabled() {
        return this.isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public List<Datalog> getLogs() {
        return this.logs;
    }

    public void setLogs(List<Datalog> logs) {
        this.logs = logs;
    }

    public String getSurlHash() {
        return this.surlHash;
    }

    public void setSurlHash(String surlHash) {
        this.surlHash = surlHash;
    }

    public String getLongUrlHash() {
        return this.longUrlHash;
    }

    public void setLongUrlHash(String longUrlHash) {
        this.longUrlHash = longUrlHash;
    }

    // Custom function to update variables when a short url is accessed
    public String accessUrl(String ip, String browserHeaders) {
        if (isEnabled) {
            Datalog datalog = new Datalog(ip, browserHeaders);
            this.logs.add(datalog);
            this.hits = this.hits + 1;
            this.lastHit = LocalDateTime.now();
            return this.longUrl;
        } else {
            return null;
        }
    }
}
