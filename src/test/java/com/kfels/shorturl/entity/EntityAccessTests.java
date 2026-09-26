package com.kfels.shorturl.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class EntityAccessTests {

    @Test
    void enabledShorturlAccessReturnsTargetAndRecordsHit() {
        Shorturl shorturl = new Shorturl("https://example.test/destination", "192.0.2.1", "abcde");
        shorturl.setEnabled(true);
        LocalDateTime beforeAccess = LocalDateTime.now();

        assertEquals("https://example.test/destination", shorturl.accessUrl("198.51.100.2", "browser/1"));

        assertEquals(1, shorturl.getHits());
        assertEquals(1, shorturl.getLogs().size());
        assertEquals("198.51.100.2", shorturl.getLogs().get(0).getHitIp());
        assertEquals("browser/1", shorturl.getLogs().get(0).getBrowserHeaders());
        assertNotNull(shorturl.getLastHit());
        assertFalse(shorturl.getLastHit().isBefore(beforeAccess));
        assertFalse(shorturl.getLastHit().isAfter(LocalDateTime.now()));
    }

    @Test
    void disabledShorturlAccessDoesNotRecordHit() {
        Shorturl shorturl = new Shorturl("https://example.test/destination", "192.0.2.1", "abcde");
        shorturl.setEnabled(false);

        assertNull(shorturl.accessUrl("198.51.100.2", "browser/1"));

        assertEquals(0, shorturl.getHits());
        assertTrue(shorturl.getLogs().isEmpty());
        assertNull(shorturl.getLastHit());
    }

    @Test
    void activeFileAccessReturnsStoredNameAndRecordsHit() {
        UploadedFile file = new UploadedFile();
        file.setActive(true);
        file.setFileName("stored-file");
        file.setLogs(new ArrayList<>());
        LocalDateTime beforeAccess = LocalDateTime.now();

        assertEquals("stored-file", file.accessFile("198.51.100.3", "browser/2"));

        assertEquals(1, file.getHits());
        assertEquals(1, file.getLogs().size());
        assertEquals("198.51.100.3", file.getLogs().get(0).getHitIp());
        assertEquals("browser/2", file.getLogs().get(0).getBrowserHeaders());
        assertNotNull(file.getLastHit());
        assertFalse(file.getLastHit().isBefore(beforeAccess));
        assertFalse(file.getLastHit().isAfter(LocalDateTime.now()));
    }

    @Test
    void inactiveFileAccessDoesNotRecordHit() {
        UploadedFile file = new UploadedFile();
        file.setActive(false);
        file.setFileName("stored-file");
        file.setLogs(new ArrayList<>());

        assertNull(file.accessFile("198.51.100.3", "browser/2"));

        assertEquals(0, file.getHits());
        assertTrue(file.getLogs().isEmpty());
        assertNull(file.getLastHit());
    }

    @Test
    void datalogConstructorCapturesRequestDetailsAndTimestamp() {
        LocalDateTime beforeConstruction = LocalDateTime.now();

        Datalog log = new Datalog("203.0.113.8", "browser/3");

        assertEquals("203.0.113.8", log.getHitIp());
        assertEquals("browser/3", log.getBrowserHeaders());
        assertNotNull(log.getHitTime());
        assertFalse(log.getHitTime().isBefore(beforeConstruction));
        assertFalse(log.getHitTime().isAfter(LocalDateTime.now()));
    }
}
