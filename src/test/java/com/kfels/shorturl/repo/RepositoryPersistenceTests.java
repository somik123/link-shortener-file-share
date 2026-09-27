package com.kfels.shorturl.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.kfels.shorturl.entity.Datalog;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;

import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RepositoryPersistenceTests {

    @Autowired
    private ShorturlRepo shorturlRepo;

    @Autowired
    private UploadedFileRepo uploadedFileRepo;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shorturlPersistsAndDerivedQueriesMatchEachConfiguredField() {
        Shorturl first = new Shorturl("https://example.test/one", "192.0.2.10", "one01");
        first.setEnabled(true);
        first.setLogs(List.of(new Datalog("198.51.100.10", "browser/one")));
        Shorturl second = new Shorturl("https://example.test/two", "192.0.2.20", "two02");
        second.setEnabled(false);

        shorturlRepo.save(first);
        shorturlRepo.save(second);
        entityManager.flush();
        entityManager.clear();

        Shorturl persisted = shorturlRepo.findBySurl("one01").get(0);
        assertNotEquals(0, persisted.getId());
        assertEquals("https://example.test/one", persisted.getLongUrl());
        assertEquals("198.51.100.10", persisted.getLogs().get(0).getHitIp());
        assertEquals(1, shorturlRepo.findBySurlHash(first.getSurlHash()).size());
        assertEquals(List.of("one01"),
                shorturlRepo.findBySurlHash(first.getSurlHash()).stream().map(Shorturl::getSurl).toList());
        assertEquals(List.of("one01"), shorturlRepo.findByLongUrl("https://example.test/one")
                .stream().map(Shorturl::getSurl).toList());
        assertEquals(List.of("one01"), shorturlRepo.findByLongUrlHash(first.getLongUrlHash())
                .stream().map(Shorturl::getSurl).toList());
        assertEquals(List.of("one01"), shorturlRepo.findByCreatorIp("192.0.2.10")
                .stream().map(Shorturl::getSurl).toList());
        assertEquals(List.of("one01"), shorturlRepo.findByIsEnabled(true)
                .stream().map(Shorturl::getSurl).toList());
        assertEquals(2, shorturlRepo.findAllCustom().size());
    }

    @Test
    void uploadedFilePersistsAndDerivedQueriesReturnMatchingRecords() {
        LocalDateTime expiry = LocalDateTime.of(2026, 10, 1, 12, 0);
        UploadedFile first = new UploadedFile();
        first.setName("first.txt");
        first.setMimeType("text/plain");
        first.setFileName("stored-first");
        first.setDownloadKey("download-first");
        first.setDownloadKeyHash("hash-first");
        first.setDeleteKey("delete-first");
        first.setCreated(LocalDateTime.of(2026, 9, 1, 12, 0));
        first.setExpiryTime(expiry);
        first.setActive(true);
        first.setCreatorIp("192.0.2.30");
        first.setHits(4);
        UploadedFile second = new UploadedFile();
        second.setName("second.txt");
        second.setDownloadKeyHash("hash-second");
        second.setExpiryTime(expiry.plusDays(1));

        uploadedFileRepo.save(first);
        uploadedFileRepo.save(second);
        entityManager.flush();
        entityManager.clear();

        UploadedFile persisted = uploadedFileRepo.findByDownloadKeyHash("hash-first").get(0);
        assertNotEquals(0, persisted.getId());
        assertEquals("first.txt", persisted.getName());
        assertEquals("text/plain", persisted.getMimeType());
        assertEquals("stored-first", persisted.getFileName());
        assertEquals("delete-first", persisted.getDeleteKey());
        assertEquals(expiry, persisted.getExpiryTime());
        assertEquals(true, persisted.getActive());
        assertEquals("192.0.2.30", persisted.getCreatorIp());
        assertEquals(4, persisted.getHits());
        assertEquals(List.of("first.txt"), uploadedFileRepo.findByExpiryTime(expiry)
                .stream().map(UploadedFile::getName).toList());
        List<UploadedFile> customResults = uploadedFileRepo.findAllCustom();
        assertEquals(2, customResults.size());
        UploadedFile customQueryResult = customResults.stream()
                .filter(file -> file.getDownloadKeyHash().equals("hash-first"))
                .findFirst().orElseThrow();
        assertEquals("first.txt", customQueryResult.getName());
        assertEquals(4, customQueryResult.getHits());
    }
}
