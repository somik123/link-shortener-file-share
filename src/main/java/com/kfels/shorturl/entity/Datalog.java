package com.kfels.shorturl.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Datalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String hitIp;
    private LocalDateTime hitTime;
    private String browserHeaders;

    public Datalog() {
        super();
    }

    public Datalog(String hitIp, String browserHeaders) {
        this.hitIp = hitIp;
        this.hitTime = LocalDateTime.now();
        this.browserHeaders = browserHeaders;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHitIp() {
        return this.hitIp;
    }

    public void setHitIp(String hitIp) {
        this.hitIp = hitIp;
    }

    public LocalDateTime getHitTime() {
        return this.hitTime;
    }

    public void setHitTime(LocalDateTime hitTime) {
        this.hitTime = hitTime;
    }

    public String getBrowserHeaders() {
        return this.browserHeaders;
    }

    public void setBrowserHeaders(String browserHeaders) {
        this.browserHeaders = browserHeaders;
    }

}
