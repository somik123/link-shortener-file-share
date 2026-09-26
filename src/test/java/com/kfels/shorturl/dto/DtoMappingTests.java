package com.kfels.shorturl.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import com.kfels.shorturl.entity.UploadedFile;

class DtoMappingTests {

    @Test
    void fileDtoConstructorMapsUploadResponseFields() {
        FileDTO dto = new FileDTO("report.pdf", "uploaded", "/file/key/report.pdf", "/delete/key/delete",
                "download-key", "delete-key", true);

        assertEquals("report.pdf", dto.getName());
        assertEquals("uploaded", dto.getMessage());
        assertEquals("/file/key/report.pdf", dto.getUrl());
        assertEquals("/delete/key/delete", dto.getDeleteUrl());
        assertEquals("download-key", dto.getDownloadKey());
        assertEquals("delete-key", dto.getDeleteKey());
        assertTrue(dto.isEnabled());
    }

    @Test
    void shorturlDtoConstructorsApplyExpectedDefaultsAndValues() {
        ShorturlDTO compact = new ShorturlDTO("abcde", false);
        assertEquals("abcde", compact.getSurl());
        assertEquals("", compact.getLongUrl());
        assertEquals("", compact.getDeleteKey());
        assertEquals(false, compact.isEnabled());

        ShorturlDTO complete = new ShorturlDTO("fghij", "https://example.test/path", "delete-key", true);
        assertEquals("fghij", complete.getSurl());
        assertEquals("https://example.test/path", complete.getLongUrl());
        assertEquals("delete-key", complete.getDeleteKey());
        assertTrue(complete.isEnabled());
    }

    @Test
    void responseDtoOverloadsPreserveContentAndDefaultOptionalFields() {
        Object content = new Object();
        ResponseDTO complete = new ResponseDTO("ERROR", content, "not found");
        assertEquals("ERROR", complete.getStatus());
        assertSame(content, complete.getContent());
        assertEquals("not found", complete.getError());

        ResponseDTO withoutError = new ResponseDTO("OK", content);
        assertEquals("OK", withoutError.getStatus());
        assertSame(content, withoutError.getContent());
        assertEquals("", withoutError.getError());

        ResponseDTO statusOnly = new ResponseDTO("OK");
        assertEquals("OK", statusOnly.getStatus());
        assertEquals("", statusOnly.getContent());
        assertEquals("", statusOnly.getError());
    }

    @Test
    void beanPropertiesAndFileLoadDtoCarryMappedValues() {
        LocalDateTime created = LocalDateTime.of(2026, 9, 26, 10, 0);
        FileDetailsDTO details = new FileDetailsDTO();
        details.setName("report.pdf");
        details.setSize("12 KB");
        details.setModified("2026-09-26T09:00:00Z");
        details.setUrl("/file/key/report.pdf");
        details.setMimeType("application/pdf");
        details.setCreatorIp("192.0.2.1");
        details.setFileName("stored-file");
        details.setDeleteUrl("/delete/key/delete");
        details.setCreated(created);
        details.setExpiryTime(created.plusDays(1));
        details.setActive(true);
        details.setDownloadKey("download-key");
        details.setHits(3);

        assertEquals("report.pdf", details.getName());
        assertEquals("12 KB", details.getSize());
        assertEquals("2026-09-26T09:00:00Z", details.getModified());
        assertEquals("/file/key/report.pdf", details.getUrl());
        assertEquals("application/pdf", details.getMimeType());
        assertEquals("192.0.2.1", details.getCreatorIp());
        assertEquals("stored-file", details.getFileName());
        assertEquals("/delete/key/delete", details.getDeleteUrl());
        assertEquals(created, details.getCreated());
        assertEquals(created.plusDays(1), details.getExpiryTime());
        assertEquals(true, details.getActive());
        assertEquals("download-key", details.getDownloadKey());
        assertEquals(3, details.getHits());

        RequestDTO request = new RequestDTO();
        request.setSurl("abcde");
        request.setUrl("https://example.test");
        assertEquals("abcde", request.getSurl());
        assertEquals("https://example.test", request.getUrl());

        UploadedFile file = new UploadedFile();
        ByteArrayResource resource = new ByteArrayResource(new byte[] { 1, 2, 3 });
        FileLoadDTO loaded = new FileLoadDTO(file, resource);
        assertSame(file, loaded.getFile());
        assertSame(resource, loaded.getResource());
    }
}
