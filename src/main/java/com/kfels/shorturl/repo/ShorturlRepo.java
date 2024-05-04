package com.kfels.shorturl.repo;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.kfels.shorturl.entity.Shorturl;

public interface ShorturlRepo extends CrudRepository<Shorturl, Integer> {
    List<Shorturl> findBySurl(String surl);

    List<Shorturl> findBySurlHash(String surlHash);

    List<Shorturl> findByLongUrl(String longUrl);

    List<Shorturl> findByLongUrlHash(String longUrlHash);

    List<Shorturl> findByCreatorIp(String creatorIp);

    List<Shorturl> findByIsEnabled(boolean isEnabled);

    @Query("SELECT s FROM Shorturl s")
    List<Shorturl> findAllCustom();
}
