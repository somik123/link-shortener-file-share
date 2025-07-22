package com.kfels.shorturl.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kfels.shorturl.utils.CommonUtils;

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
import lombok.Data;

@Data
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
            surl = CommonUtils.generateStringForShorturl("");

        this.surl = surl;
        this.surlHash = CommonUtils.getHash(surl);
        this.longUrl = longUrl;
        this.longUrlHash = CommonUtils.getHash(longUrl);
        this.deleteKey = CommonUtils.randString(32, 3);
        this.creatorIp = creatorIp;
        this.hits = 0;
        this.created = LocalDateTime.now();
        this.lastHit = null;
        this.isEnabled = true;
        this.logs = new ArrayList<>();
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
