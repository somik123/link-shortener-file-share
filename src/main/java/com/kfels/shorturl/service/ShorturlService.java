package com.kfels.shorturl.service;

import java.util.List;

import com.kfels.shorturl.entity.Shorturl;

public interface ShorturlService {

    // Public options
    Shorturl generateShorturl(String longUrl, String creatorIp, String surl);

    Shorturl generateShorturl(String longUrl, String creatorIp);

    String accessShorturl(String surl, String ip, String browserHeaders);

    boolean deleteShorturl(String surl, String deleteKey);

    // Admin options
    List<Shorturl> getAllShorturls();

    Shorturl getShorturlDetails(String surl);

    boolean enableShorturl(String surl);

    boolean disableShorturl(String surl);

    boolean deleteShorturl(String surl);

    boolean isSurlUnique(String surl);

    Shorturl getShorturlByLongurl(String longUrl);
}
