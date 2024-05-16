package com.kfels.shorturl.service;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.repo.ShorturlRepo;
import com.kfels.shorturl.utils.CommonUtils;

@Service
public class ShorturlServiceImpl implements ShorturlService {

    @Autowired
    ShorturlRepo shorturlRepo;

    Logger log = Logger.getLogger(ShorturlService.class.getName());

    @Override
    public Shorturl generateShorturl(String longUrl, String creatorIp, String surl) {
        // Ensure required data is there
        if (creatorIp == null || creatorIp.length() == 0)
            return null;
        // If not provided, generate a shorturl
        surl = CommonUtils.generateStringForShorturl(surl);
        // Ensure shorturl is unique, otherwise generate a random one
        while (!isSurlUnique(surl)) {
            surl = CommonUtils.generateStringForShorturl(null);
        }
        Shorturl shorturl = new Shorturl(longUrl, creatorIp, surl);
        shorturlRepo.save(shorturl);
        return shorturl;
    }

    @Override
    public Shorturl generateShorturl(String longUrl, String creatorIp) {
        return generateShorturl(longUrl, creatorIp, null);
    }

    @Override
    public String accessShorturl(String surl, String ip, String browserHeaders) {
        Shorturl shorturl = getShorturlDetails(surl);
        if (shorturl == null || ip == null || ip.length() == 0 || browserHeaders == null
                || browserHeaders.length() == 0)
            return null;
        String url = shorturl.accessUrl(ip, browserHeaders);
        shorturlRepo.save(shorturl);
        return url;
    }

    @Override
    public boolean deleteShorturl(String surl, String deleteKey) {
        Shorturl shorturl = getShorturlDetails(surl);
        if (shorturl == null || deleteKey == null || deleteKey.length() == 0)
            return false;
        else if (shorturl.getDeleteKey().equals(deleteKey)) {
            shorturlRepo.delete(shorturl);
            return true;
        } else
            return false;
    }

    @Override
    public List<Shorturl> getAllShorturls() {
        return shorturlRepo.findAllCustom();
    }

    @Override
    public Shorturl getShorturlDetails(String surl) {
        if (surl == null || surl.length() == 0)
            return null;
        String surlHash = CommonUtils.getHash(surl);
        List<Shorturl> sList = shorturlRepo.findBySurlHash(surlHash);
        if (sList == null || sList.size() == 0)
            return null;
        Shorturl shorturl = sList.get(0);
        return shorturl;
    }

    @Override
    public boolean enableShorturl(String surl) {
        Shorturl shorturl = getShorturlDetails(surl);
        if (shorturl != null) {
            shorturl.setIsEnabled(true);
            shorturlRepo.save(shorturl);
            return true;
        } else
            return true;
    }

    @Override
    public boolean disableShorturl(String surl) {
        Shorturl shorturl = getShorturlDetails(surl);
        if (shorturl != null) {
            shorturl.setIsEnabled(false);
            shorturlRepo.save(shorturl);
            return true;
        } else
            return true;
    }

    @Override
    public boolean deleteShorturl(String surl) {
        Shorturl shorturl = getShorturlDetails(surl);
        if (shorturl != null) {
            shorturlRepo.delete(shorturl);
            return true;
        } else
            return true;
    }

    @Override
    public Shorturl getShorturlByLongurl(String longUrl) {
        if (longUrl == null || longUrl.length() == 0)
            return null;
        String longUrlHash = CommonUtils.getHash(longUrl);
        List<Shorturl> sList = shorturlRepo.findByLongUrlHash(longUrlHash);
        if (sList == null || sList.size() == 0)
            return null;
        Shorturl shorturl = sList.get(0);
        return shorturl;
    }

    @Override
    public boolean isSurlUnique(String surl) {
        return (getShorturlDetails(surl) == null) ? true : false;
    }
}
