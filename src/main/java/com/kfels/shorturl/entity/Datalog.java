package com.kfels.shorturl.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
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
}
