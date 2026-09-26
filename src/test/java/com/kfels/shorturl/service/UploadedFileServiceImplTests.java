package com.kfels.shorturl.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import com.kfels.shorturl.dto.FileDTO;
import com.kfels.shorturl.dto.FileDetailsDTO;
import com.kfels.shorturl.dto.FileLoadDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.repo.UploadedFileRepo;
import com.kfels.shorturl.utils.CommonUtils;

class UploadedFileServiceImplTests {

    private static final Path STORAGE_PATH = Path.of("data", "uploads");

    private UploadedFileRepo repo;
    private UploadedFileServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(UploadedFileRepo.class);
        service = new UploadedFileServiceImpl(repo);
    }

    @Test
    void savesUploadedFileAndReturnsDownloadAndDeleteLinks() throws IOException {
        Files.createDirectories(STORAGE_PATH);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "read me.txt", "text/plain", "sample content".getBytes());

        FileDTO result = service.saveUploadedFile(multipartFile, "192.0.2.11", 60);

        assertNotNull(result);
        assertEquals("read_me.txt", result.getName());
        assertEquals("File uploaded successfully.", result.getMessage());
        ArgumentCaptor<UploadedFile> saved = ArgumentCaptor.forClass(UploadedFile.class);
        verify(repo).save(saved.capture());
        UploadedFile record = saved.getValue();
        Path storedFile = STORAGE_PATH.resolve(record.getFileName());
        try {
            assertTrue(Files.exists(storedFile));
            assertEquals("sample content", Files.readString(storedFile));
            assertEquals(record.getDownloadKey(), result.getDownloadKey());
            assertEquals(record.getDeleteKey(), result.getDeleteKey());
            assertEquals("/file/" + record.getDownloadKey() + "/" + record.getDownloadKey() + ".txt",
                    result.getUrl());
            assertEquals("/deleteFile/" + record.getDownloadKey() + "/" + record.getDeleteKey(),
                    result.getDeleteUrl());
            assertEquals("192.0.2.11", record.getCreatorIp());
            assertEquals("text/plain", record.getMimeType());
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void rejectsUploadedFileWithFilenameRemovedBySanitization() {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "??", "text/plain", new byte[] { 1 });

        assertNull(service.saveUploadedFile(multipartFile, "192.0.2.12", 60));

        verifyNoInteractions(repo);
    }

    @Test
    void rejectsUploadedFilenameContainingTraversalDots() {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bad..txt", "text/plain", new byte[] { 1 });

        assertNull(service.saveUploadedFile(multipartFile, "192.0.2.23", 60));

        verifyNoInteractions(repo);
    }

    @Test
    void usesDefaultExpiryForValuesOutsideSupportedRange() throws IOException {
        Files.createDirectories(STORAGE_PATH);
        LocalDateTime before = LocalDateTime.now();
        MockMultipartFile negativeExpiryFile = new MockMultipartFile(
                "file", "negative-expiry.txt", "text/plain", new byte[] { 1 });
        MockMultipartFile excessiveExpiryFile = new MockMultipartFile(
                "file", "excessive-expiry.txt", "text/plain", new byte[] { 2 });

        FileDTO negativeResult = service.saveUploadedFile(negativeExpiryFile, "192.0.2.24", -1);
        LocalDateTime betweenCalls = LocalDateTime.now();
        FileDTO excessiveResult = service.saveUploadedFile(excessiveExpiryFile, "192.0.2.25", 5_259_601);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<UploadedFile> saved = ArgumentCaptor.forClass(UploadedFile.class);
        verify(repo, org.mockito.Mockito.times(2)).save(saved.capture());
        List<UploadedFile> records = saved.getAllValues();
        Path firstStored = STORAGE_PATH.resolve(records.get(0).getFileName());
        Path secondStored = STORAGE_PATH.resolve(records.get(1).getFileName());
        try {
            assertNotNull(negativeResult);
            assertNotNull(excessiveResult);
            LocalDateTime expectedFirstExpiry = before.plusMinutes(43_200);
            LocalDateTime expectedSecondExpiry = betweenCalls.plusMinutes(43_200);
            assertFalse(records.get(0).getExpiryTime().isBefore(expectedFirstExpiry));
            assertFalse(records.get(0).getExpiryTime().isAfter(betweenCalls.plusMinutes(43_200)));
            assertFalse(records.get(1).getExpiryTime().isBefore(expectedSecondExpiry));
            assertFalse(records.get(1).getExpiryTime().isAfter(after.plusMinutes(43_200)));
        } finally {
            Files.deleteIfExists(firstStored);
            Files.deleteIfExists(secondStored);
        }
    }

    @Test
    void returnsNullWhenUploadedFileStreamCannotBeRead() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(1L);
        when(multipartFile.getOriginalFilename()).thenReturn("unreadable.txt");
        doThrow(new IOException("simulated read failure")).when(multipartFile).getInputStream();

        assertNull(service.saveUploadedFile(multipartFile, "192.0.2.26", 60));

        verify(multipartFile).getInputStream();
        verifyNoInteractions(repo);
    }

    @Test
    void savesTelegramFileAndRemovesItsSourceAfterCopying() throws IOException {
        Files.createDirectories(STORAGE_PATH);
        Path source = Files.createTempFile("telegram-upload-", ".txt");
        Files.writeString(source, "telegram content");
        String sourceArgument = source.toAbsolutePath().toString().replace('\\', '/');
        Path storedFile = null;
        try {
            UploadedFile result = service.saveFromTelegram(sourceArgument, 2);

            assertNotNull(result);
            assertEquals("Telegram", result.getCreatorIp());
            assertEquals(source.getFileName().toString(), result.getName());
            verify(repo).save(result);
            storedFile = STORAGE_PATH.resolve(result.getFileName());
            assertTrue(Files.exists(storedFile));
            assertEquals("telegram content", Files.readString(storedFile));
            assertFalse(Files.exists(source));
        } finally {
            if (storedFile != null)
                Files.deleteIfExists(storedFile);
            Files.deleteIfExists(source);
        }
    }

    @Test
    void returnsNullForMissingTelegramSource() {
        assertNull(service.saveFromTelegram("missing-telegram-upload.txt", 1));
        verifyNoInteractions(repo);
    }

    @Test
    void rejectsTelegramDirectoryPathWithNoFilename() {
        String directoryWithTrailingSeparator = STORAGE_PATH.toAbsolutePath().toString() + "/";

        assertNull(service.saveFromTelegram(directoryWithTrailingSeparator, 1));

        verifyNoInteractions(repo);
    }

    @Test
    void loadsActiveFileAndPersistsHitDetails() throws IOException {
        Path file = createStoredFile("load-test-" + System.nanoTime(), "download content");
        try {
            UploadedFile uploadedFile = fileRecord(file, true);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            FileLoadDTO result = service.load("sampleKey", "192.0.2.13", "browser");

            assertNotNull(result);
            assertSame(uploadedFile, result.getFile());
            assertTrue(result.getResource().exists());
            assertEquals(1, uploadedFile.getHits());
            assertEquals("192.0.2.13", uploadedFile.getLogs().get(0).getHitIp());
            verify(repo).save(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void loadReturnsNullForMissingRecordOrMissingPhysicalFile() throws IOException {
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("missingKey"))).thenReturn(List.of());
        assertNull(service.load("missingKey", "192.0.2.14", "browser"));

        Path missingFile = STORAGE_PATH.resolve("not-present-" + System.nanoTime());
        UploadedFile uploadedFile = fileRecord(missingFile, true);
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("missingDiskFile"))).thenReturn(List.of(uploadedFile));
        assertNull(service.load("missingDiskFile", "192.0.2.14", "browser"));
        verify(repo, never()).save(any());
    }

    @Test
    void loadReturnsNullWhenRecordCannotRecordAccessDetails() throws IOException {
        Path file = createStoredFile("load-error-" + System.nanoTime(), "download content");
        try {
            UploadedFile uploadedFile = fileRecord(file, true);
            uploadedFile.setLogs(null);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            assertNull(service.load("sampleKey", "192.0.2.27", "browser"));

            verify(repo, never()).save(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void deleteReturnsFalseWhenStoredFileDoesNotExist() {
        Path missingFile = STORAGE_PATH.resolve("not-present-" + System.nanoTime());
        UploadedFile uploadedFile = fileRecord(missingFile, true);
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("missingDiskFile"))).thenReturn(List.of(uploadedFile));

        assertFalse(service.delete("missingDiskFile", "validDeleteKey"));

        verify(repo).delete(uploadedFile);
    }

    @Test
    void deleteRejectsNullKeyWithoutRemovingRecord() throws IOException {
        Path file = createStoredFile("delete-test-" + System.nanoTime(), "content");
        try {
            UploadedFile uploadedFile = fileRecord(file, true);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            assertFalse(service.delete("sampleKey", null));
            assertTrue(Files.exists(file));
            verify(repo, never()).delete(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void returnsMimeTypeForActiveFileAndNullForMissingFile() {
        UploadedFile uploadedFile = fileRecord(STORAGE_PATH.resolve("mime-type-test"), true);
        uploadedFile.setMimeType("application/octet-stream");
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("missingKey"))).thenReturn(List.of());

        assertEquals("application/octet-stream", service.getMimeType("sampleKey"));
        assertNull(service.getMimeType("missingKey"));
    }

    @Test
    void looksUpFilesByDownloadKeyAndAllowsAdminOverrideOfInactiveFiles() {
        UploadedFile inactiveFile = fileRecord(STORAGE_PATH.resolve("inactive-record"), false);
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(inactiveFile));

        assertNull(service.getUploadFileFromDownloadKey("sampleKey"));
        assertSame(inactiveFile, service.getUploadFileFromDownloadKey("sampleKey", true));
        assertNull(service.getUploadFileFromDownloadKey(null));
        assertNull(service.getUploadFileFromDownloadKey("ab", true));
        verify(repo, never()).findByDownloadKeyHash(null);
    }

    @Test
    void returnsNullForUnknownDownloadKeyFromNullOrEmptyRepositoryResults() {
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("nullListKey"))).thenReturn(null);
        when(repo.findByDownloadKeyHash(CommonUtils.getHash("emptyListKey"))).thenReturn(List.of());

        assertNull(service.getUploadFileFromDownloadKey("nullListKey"));
        assertNull(service.getUploadFileFromDownloadKey("emptyListKey"));
    }

    @Test
    void returnsAdminFileDetailsNewestFirstIncludingDiskMetadata() throws IOException {
        Path olderDiskFile = createStoredFile("details-older-" + System.nanoTime(), "old");
        Path newerDiskFile = createStoredFile("details-newer-" + System.nanoTime(), "newer content");
        try {
            UploadedFile older = fileRecord(olderDiskFile, true);
            older.setName("old%20file.txt");
            older.setCreated(LocalDateTime.of(2024, 1, 1, 0, 0));
            older.setExpiryTime(LocalDateTime.of(2024, 2, 1, 0, 0));
            older.setHits(2);
            UploadedFile newer = fileRecord(newerDiskFile, false);
            newer.setName("new%20file.bin");
            newer.setCreated(LocalDateTime.of(2025, 1, 1, 0, 0));
            when(repo.findAllCustom()).thenReturn(new ArrayList<>(List.of(older, newer)));

            List<FileDetailsDTO> result = service.getAllFileDetails();

            assertEquals(2, result.size());
            assertEquals(newer.getDownloadKey(), result.get(0).getDownloadKey());
            assertEquals(older.getDownloadKey(), result.get(1).getDownloadKey());
            FileDetailsDTO details = result.get(1);
            assertEquals("old file.txt", details.getName());
            assertEquals(older.getFileName(), details.getFileName());
            assertEquals(older.getMimeType(), details.getMimeType());
            assertEquals(older.getCreatorIp(), details.getCreatorIp());
            assertEquals(older.getCreated(), details.getCreated());
            assertEquals(older.getExpiryTime(), details.getExpiryTime());
            assertEquals(older.getDownloadKey(), details.getDownloadKey());
            assertEquals(2, details.getHits());
            assertEquals(CommonUtils.formatSize(3), details.getSize());
            assertNotNull(details.getModified());
            assertEquals("/file/" + older.getDownloadKey() + "/" + older.getDownloadKey() + ".txt",
                    details.getUrl());
            assertEquals("/deleteFile/" + older.getDownloadKey() + "/" + older.getDeleteKey(),
                    details.getDeleteUrl());
        } finally {
            Files.deleteIfExists(olderDiskFile);
            Files.deleteIfExists(newerDiskFile);
        }
    }

    @Test
    void returnsEmptyAdminDetailsWhenRepositoryHasNoFilesOrReturnsNull() {
        when(repo.findAllCustom()).thenReturn(List.of());
        assertTrue(service.getAllFileDetails().isEmpty());
        when(repo.findAllCustom()).thenReturn(null);
        assertTrue(service.getAllFileDetails().isEmpty());
    }

    @Test
    void adminDetailsAreReturnedWithoutDiskMetadataForMissingFile() {
        UploadedFile record = fileRecord(STORAGE_PATH.resolve("missing-details-" + System.nanoTime()), true);
        record.setName("missing.txt");
        when(repo.findAllCustom()).thenReturn(List.of(record));

        FileDetailsDTO result = service.getAllFileDetails().get(0);

        assertNull(result.getSize());
        assertNull(result.getModified());
    }

    @Test
    void cronJobReturnsOkForEmptyRepository() {
        when(repo.findAllCustom()).thenReturn(List.of());

        ResponseDTO response = service.cronJobs();

        assertEquals("OK", response.getStatus());
        assertEquals("", response.getContent());
        assertEquals("", response.getError());
    }

    @Test
    void cronJobReturnsOkWhenRepositoryReturnsNull() {
        when(repo.findAllCustom()).thenReturn(null);

        ResponseDTO response = service.cronJobs();

        assertEquals("OK", response.getStatus());
        assertEquals("", response.getContent());
        assertEquals("", response.getError());
    }

    @Test
    void cronJobKeepsUnexpiredFileAndDeletesExpiredFile() throws IOException {
        Path expiredPath = createStoredFile("expired-cron-" + System.nanoTime(), "expired");
        Path futurePath = createStoredFile("future-cron-" + System.nanoTime(), "future");
        try {
            UploadedFile expired = fileRecord(expiredPath, true);
            expired.setExpiryTime(LocalDateTime.now().minusMinutes(1));
            UploadedFile future = fileRecord(futurePath, true);
            future.setExpiryTime(LocalDateTime.now().plusHours(1));
            when(repo.findAllCustom()).thenReturn(new ArrayList<>(List.of(expired, future)));
            when(repo.findByDownloadKeyHash(CommonUtils.getHash(expired.getDownloadKey())))
                    .thenReturn(List.of(expired));

            ResponseDTO response = service.cronJobs();

            assertEquals("OK", response.getStatus());
            assertTrue(response.getError().contains("removed 1 objects."));
            assertFalse(Files.exists(expiredPath));
            assertTrue(Files.exists(futurePath));
            verify(repo).delete(expired);
            verify(repo, never()).delete(future);
        } finally {
            Files.deleteIfExists(expiredPath);
            Files.deleteIfExists(futurePath);
        }
    }

    @Test
    void savesNonNullRecordAndIgnoresNull() {
        UploadedFile uploadedFile = fileRecord(STORAGE_PATH.resolve("saved-record"), true);

        assertSame(uploadedFile, service.save(uploadedFile));
        assertNull(service.save(null));

        ArgumentCaptor<UploadedFile> saved = ArgumentCaptor.forClass(UploadedFile.class);
        verify(repo).save(saved.capture());
        assertSame(uploadedFile, saved.getValue());
    }

    @Test
    void deletesInactiveFileWithValidDeleteKey() throws IOException {
        Path file = Files.createTempFile(Files.createDirectories(Path.of("data", "uploads")), "delete-test-", ".tmp");
        try {
            UploadedFileRepo repo = mock(UploadedFileRepo.class);
            UploadedFile uploadedFile = fileRecord(file, false);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            UploadedFileServiceImpl service = new UploadedFileServiceImpl(repo);
            assertTrue(service.delete("sampleKey", "validDeleteKey"));
            assertFalse(Files.exists(file));
            verify(repo).delete(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsWrongDeleteKeyForInactiveFile() throws IOException {
        Path file = Files.createTempFile(Files.createDirectories(Path.of("data", "uploads")), "delete-test-", ".tmp");
        try {
            UploadedFileRepo repo = mock(UploadedFileRepo.class);
            UploadedFile uploadedFile = fileRecord(file, false);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            assertFalse(new UploadedFileServiceImpl(repo).delete("sampleKey", "wrongKey"));
            assertTrue(Files.exists(file));
            verify(repo, never()).delete(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void deletingActiveFileStillWorks() throws IOException {
        Path file = Files.createTempFile(Files.createDirectories(Path.of("data", "uploads")), "delete-test-", ".tmp");
        try {
            UploadedFileRepo repo = mock(UploadedFileRepo.class);
            UploadedFile uploadedFile = fileRecord(file, true);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            assertTrue(new UploadedFileServiceImpl(repo).delete("sampleKey", "validDeleteKey"));
            assertFalse(Files.exists(file));
            verify(repo).delete(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void inactiveFileRemainsUnavailableForDownload() throws IOException {
        Path file = Files.createTempFile(Files.createDirectories(Path.of("data", "uploads")), "delete-test-", ".tmp");
        try {
            UploadedFileRepo repo = mock(UploadedFileRepo.class);
            UploadedFile uploadedFile = fileRecord(file, false);
            when(repo.findByDownloadKeyHash(CommonUtils.getHash("sampleKey"))).thenReturn(List.of(uploadedFile));

            assertNull(new UploadedFileServiceImpl(repo).load("sampleKey", "127.0.0.1", "browser"));
            assertTrue(Files.exists(file));
            verify(repo, never()).save(uploadedFile);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private UploadedFile fileRecord(Path file, boolean active) {
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setName(file.getFileName().toString());
        uploadedFile.setMimeType("text/plain");
        uploadedFile.setFileName(file.getFileName().toString());
        uploadedFile.setDownloadKey("sampleKey");
        uploadedFile.setDownloadKeyHash(CommonUtils.getHash("sampleKey"));
        uploadedFile.setDeleteKey("validDeleteKey");
        uploadedFile.setActive(active);
        uploadedFile.setCreatorIp("192.0.2.20");
        uploadedFile.setCreated(LocalDateTime.now());
        uploadedFile.setExpiryTime(LocalDateTime.now().plusHours(1));
        uploadedFile.setLogs(new ArrayList<>());
        return uploadedFile;
    }

    private Path createStoredFile(String fileName, String content) throws IOException {
        Files.createDirectories(STORAGE_PATH);
        Path file = STORAGE_PATH.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
