package com.kfels.shorturl.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.telegram.ApiRequestHandler;
import com.kfels.shorturl.telegram.Telegram;
import com.kfels.shorturl.utils.CommonUtils;

class TelegramControllerTests {

    private ShorturlService surlService;
    private UploadedFileService fileService;
    private TelegramController controller;

    @BeforeEach
    void setUp() {
        surlService = mock(ShorturlService.class);
        fileService = mock(UploadedFileService.class);
        controller = new TelegramController(surlService, fileService);
    }

    @Test
    void rejectsInvalidWebhookTokenBeforeProcessingUpdate() {
        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) ->
                when(telegram.isValidToken("wrong-token")).thenReturn(false))) {
            assertEquals("", controller.receiveWebHook("{}", "wrong-token"));
            verify(telegrams.constructed().get(0)).isValidToken("wrong-token");
            verify(telegrams.constructed().get(0), never()).sendMessage(org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void deleteFileCommandDelegatesAndRepliesWithOutcome() {
        when(fileService.delete("download", "delete-key")).thenReturn(false);
        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(true);
            when(telegram.getMessageText()).thenReturn("/deleteFile_download_delete-key");
            when(telegram.getChatId()).thenReturn(42);
        })) {
            assertEquals("", controller.receiveWebHook("{}", null));

            Telegram telegram = telegrams.constructed().get(0);
            verify(fileService).delete("download", "delete-key");
            verify(telegram).sendMessage(42, "Fail");
        }
    }

    @Test
    void deleteShorturlCommandDelegatesAndRepliesWithOutcome() {
        when(surlService.deleteShorturl("alias", "delete-key")).thenReturn(true);
        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(true);
            when(telegram.getMessageText()).thenReturn("/deleteSURL_alias_delete-key");
            when(telegram.getChatId()).thenReturn(42);
        })) {
            controller.receiveWebHook("{}", null);

            verify(surlService).deleteShorturl("alias", "delete-key");
            verify(telegrams.constructed().get(0)).sendMessage(42, "Success");
        }
    }

    @Test
    void enableCommandsActivateAndPersistExistingRecords() {
        Shorturl shorturl = new Shorturl();
        shorturl.setSurl("alias");
        shorturl.setLongUrl("https://example.com");
        shorturl.setEnabled(false);
        when(surlService.getShorturlDetails("alias")).thenReturn(shorturl);
        UploadedFile file = new UploadedFile();
        file.setId(12);
        file.setDownloadKey("download");
        when(fileService.getUploadFileFromDownloadKey("download")).thenReturn(file);

        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(true);
            when(telegram.getMessageText()).thenReturn("/enableSURL_alias");
            when(telegram.getChatId()).thenReturn(42);
        })) {
            controller.receiveWebHook("{}", null);
            assertEquals(true, shorturl.isEnabled());
            verify(surlService).save(shorturl);
            verify(telegrams.constructed().get(0)).sendMessage(42,
                    "Shorturl alias enabled for:\nhttps://example.com");
        }

        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(true);
            when(telegram.getMessageText()).thenReturn("/enableFile_download");
            when(telegram.getChatId()).thenReturn(42);
        })) {
            controller.receiveWebHook("{}", null);
            assertEquals(true, file.isActive());
            verify(fileService).save(file);
            verify(telegrams.constructed().get(0)).sendMessage(42, "File 12 is enabled for download.");
        }
    }

    @Test
    void helpCommandReturnsAvailableCommandsAndPlainTextWithoutUrlIsIgnored() {
        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(true);
            when(telegram.getMessageText()).thenReturn("/help");
            when(telegram.getChatId()).thenReturn(7);
        })) {
            controller.receiveWebHook("{}", null);
            verify(telegrams.constructed().get(0)).sendMessage(eq(7),
                    org.mockito.ArgumentMatchers.contains("/deleteSURL_{shortUrlId}_{deleteKey}"));
        }

        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
            when(telegram.isCommand()).thenReturn(false);
            when(telegram.getMessageText()).thenReturn("not a URL");
        })) {
            assertEquals("", controller.receiveWebHook("{}", null));
            verifyNoServiceInteractions();
        }
    }

    @Test
    void parsedWebhookUrlGeneratesShorturlWithoutExternalRequests() {
        String longUrl = "https://example.com/from-telegram";
        Shorturl generated = new Shorturl();
        generated.setSurl("tg-alias");
        generated.setLongUrl(longUrl);
        generated.setDeleteKey("delete-secret");
        generated.setEnabled(true);
        when(surlService.getShorturlByLongurl(longUrl)).thenReturn(null);
        when(surlService.generateShorturl(longUrl, "Telegram")).thenReturn(generated);

        String update = """
                {"update_id":1,"message":{"message_id":2,"chat":{"id":42},"text":"https://example.com/from-telegram"}}
                """;
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class,
                    org.mockito.Mockito.CALLS_REAL_METHODS);
                MockedStatic<ApiRequestHandler> apiRequests = mockStatic(ApiRequestHandler.class)) {
            commonUtils.when(() -> CommonUtils.asynSendTelegramMessage(anyString())).thenAnswer(invocation -> null);
            apiRequests.when(() -> ApiRequestHandler.postRequest(anyString(), anyMap())).thenReturn("{\"ok\":true,}");

            assertEquals("", controller.receiveWebHook(update, null));

            verify(surlService).getShorturlByLongurl(longUrl);
            verify(surlService).generateShorturl(longUrl, "Telegram");
            if (hasTelegramCredentials()) {
                apiRequests.verify(() -> ApiRequestHandler.postRequest(contains("/sendMessage"), anyMap()));
            } else {
                apiRequests.verifyNoInteractions();
            }
        }
    }

    @Test
    void shortenFileCommandBuildsFileUrlAndCreatesShorturl() {
        UploadedFile file = new UploadedFile();
        file.setDownloadKey("download-key");
        file.setName("notes.txt");
        when(fileService.getUploadFileFromDownloadKey("download-key")).thenReturn(file);

        String fileUrl = String.format("%sfile/download-key/notes.txt", System.getenv("SITE_FULL_URL"));
        Shorturl generated = new Shorturl();
        generated.setSurl("file-alias");
        generated.setLongUrl(fileUrl);
        generated.setDeleteKey("delete-secret");
        generated.setEnabled(true);
        when(surlService.getShorturlByLongurl(fileUrl)).thenReturn(null);
        when(surlService.generateShorturl(fileUrl, "Telegram")).thenReturn(generated);

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class,
                    org.mockito.Mockito.CALLS_REAL_METHODS);
                MockedStatic<ApiRequestHandler> apiRequests = mockStatic(ApiRequestHandler.class);
                MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
                    when(telegram.isCommand()).thenReturn(true);
                    when(telegram.getMessageText()).thenReturn("/shorten_download-key");
                    when(telegram.getAdminId()).thenReturn(42);
                    when(telegram.getChatId()).thenReturn(42);
                })) {
            commonUtils.when(() -> CommonUtils.asynSendTelegramMessage(anyString())).thenAnswer(invocation -> null);
            apiRequests.when(() -> ApiRequestHandler.postRequest(anyString(), anyMap())).thenReturn("{\"ok\":true,}");

            assertEquals("", controller.receiveWebHook("{}", null));

            verify(fileService).getUploadFileFromDownloadKey("download-key");
            verify(surlService).getShorturlByLongurl(fileUrl);
            verify(surlService).generateShorturl(fileUrl, "Telegram");
            verify(telegrams.constructed().get(0)).sendMessage(eq(42),
                    org.mockito.ArgumentMatchers.contains("ShortURL:"));
        }
    }

    @Test
    void parsedDocumentUpdateIsStoredWithoutNeedingTelegramCredentials() {
        String update = """
                {"update_id":3,"message":{"message_id":4,"chat":{"id":42},"document":{"file_id":"file-123","file_name":"notes.txt","mime_type":"text/plain","file_size":5}}}
                """;
        Telegram parsedUpdate = new Telegram(update);
        assertEquals("doc", parsedUpdate.getFileType());
        assertEquals(42, parsedUpdate.getChatId());

        UploadedFile savedFile = new UploadedFile();
        savedFile.setId(12);
        savedFile.setName("notes.txt");
        savedFile.setDownloadKey("download-key");
        savedFile.setDeleteKey("delete-key");
        when(fileService.saveFromTelegram("mocked/path/notes.txt", 1)).thenReturn(savedFile);

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class,
                    org.mockito.Mockito.CALLS_REAL_METHODS);
                MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) -> {
                    when(telegram.getAdminId()).thenReturn(parsedUpdate.getAdminId());
                    when(telegram.getChatId()).thenReturn(parsedUpdate.getChatId());
                    when(telegram.getMessageText()).thenReturn(parsedUpdate.getMessageText());
                    when(telegram.isCommand()).thenReturn(parsedUpdate.isCommand());
                    when(telegram.getFileType()).thenReturn(parsedUpdate.getFileType());
                    when(telegram.downloadFile(anyString())).thenReturn("mocked/path/notes.txt");
                })) {
            commonUtils.when(() -> CommonUtils.asynSendTelegramMessage(anyString())).thenAnswer(invocation -> null);

            assertEquals("", controller.receiveWebHook(update, null));

            verify(fileService).saveFromTelegram("mocked/path/notes.txt", 1);
            verify(telegrams.constructed().get(0)).sendMessage(eq(42),
                    org.mockito.ArgumentMatchers.contains("Download:"));
        }
    }

    @Test
    void parsedWebhookDocumentIsDownloadedAndStoredWhenTelegramCredentialsAreConfigured() {
        Assumptions.assumeTrue(hasTelegramCredentials(),
                "Telegram API credentials are required to enter the file-download branch");

        UploadedFile savedFile = new UploadedFile();
        savedFile.setId(12);
        savedFile.setName("notes.txt");
        savedFile.setDownloadKey("download-key");
        savedFile.setDeleteKey("delete-key");

        String update = """
                {"update_id":2,"message":{"message_id":3,"chat":{"id":42},"document":{"file_id":"file-123","file_name":"notes.txt","mime_type":"text/plain","file_size":5}}}
                """;
        try (MockedStatic<ApiRequestHandler> apiRequests = mockStatic(ApiRequestHandler.class)) {
            apiRequests.when(() -> ApiRequestHandler.getRequest(contains("getFile?file_id=file-123")))
                    .thenReturn("{\"ok\":true,\"result\":{\"file_path\":\"documents/notes.txt\"}}");
            apiRequests.when(() -> ApiRequestHandler.downloadFile(contains("/documents/notes.txt"), anyString()))
                    .thenReturn(true);
            apiRequests.when(() -> ApiRequestHandler.postRequest(anyString(), anyMap()))
                    .thenReturn("{\"ok\":true,}");
            when(fileService.saveFromTelegram(anyString(), eq(1))).thenReturn(savedFile);

            assertEquals("", controller.receiveWebHook(update, null));

            org.mockito.ArgumentCaptor<String> downloadedPath = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fileService).saveFromTelegram(downloadedPath.capture(), eq(1));
            assertTrue(downloadedPath.getValue().endsWith(".txt"));
            apiRequests.verify(() -> ApiRequestHandler.getRequest(contains("getFile?file_id=file-123")));
            apiRequests.verify(() -> ApiRequestHandler.downloadFile(contains("/documents/notes.txt"), anyString()));
            apiRequests.verify(() -> ApiRequestHandler.postRequest(contains("/sendMessage"), anyMap()));
        }
    }

    private void verifyNoServiceInteractions() {
        org.mockito.Mockito.verifyNoInteractions(surlService, fileService);
    }

    private boolean hasTelegramCredentials() {
        String apiKey = System.getenv("TELEGRAM_APIKEY");
        String adminId = System.getenv("TELEGRAM_ADMINID");
        return apiKey != null && apiKey.length() > 20 && adminId != null && !adminId.isBlank();
    }
}
