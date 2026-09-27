package com.kfels.shorturl.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.kfels.shorturl.telegram.entity.TelegramChat;
import com.kfels.shorturl.telegram.entity.TelegramEntities;
import com.kfels.shorturl.telegram.entity.TelegramFile;
import com.kfels.shorturl.telegram.entity.TelegramMessage;
import com.kfels.shorturl.telegram.entity.TelegramUpdate;

class TelegramTests {

    @Test
    void readsMessageAndSenderDetailsFromUpdate() {
        TelegramChat chat = new TelegramChat();
        chat.setId(123);
        chat.setUsername("chat-user");
        TelegramChat sender = new TelegramChat();
        sender.setId(456);
        sender.setUsername("sender");
        TelegramMessage message = new TelegramMessage();
        message.setText("/start");
        message.setChat(chat);
        message.setFrom(sender);
        TelegramEntities command = new TelegramEntities();
        command.setType("bot_command");
        message.setEntities(List.of(command));
        TelegramUpdate update = new TelegramUpdate();
        update.setMessage(message);
        Telegram telegram = new Telegram();
        telegram.setUpdate(update);

        assertEquals("/start", telegram.getMessageText());
        assertEquals(123, telegram.getChatId());
        assertEquals("chat-user", telegram.getChatUsername());
        assertEquals(456, telegram.getMessageFromId());
        assertEquals("sender", telegram.getMessageFromUsername());
        assertTrue(telegram.isCommand());
    }

    @Test
    void missingUpdateFieldsReturnDocumentedEmptyValues() {
        Telegram telegram = new Telegram();

        assertNull(telegram.getMessageText());
        assertEquals(0, telegram.getChatId());
        assertNull(telegram.getChatUsername());
        assertEquals(0, telegram.getMessageFromId());
        assertNull(telegram.getMessageFromUsername());
        assertFalse(telegram.isCommand());
        assertNull(telegram.getFileType());
        assertNull(telegram.getFileDetails());
        assertNull(telegram.getImageDetails());
    }

    @Test
    void choosesFileTypeAndLargestPhoto() {
        TelegramFile smallPhoto = file("small", 100);
        TelegramFile largePhoto = file("large", 800);
        TelegramMessage message = new TelegramMessage();
        message.setPhoto(List.of(smallPhoto, largePhoto));
        TelegramUpdate update = new TelegramUpdate();
        update.setMessage(message);
        Telegram telegram = new Telegram();
        telegram.setUpdate(update);

        assertEquals("image", telegram.getFileType());
        assertEquals(largePhoto, telegram.getImageDetails());
        assertEquals(largePhoto, telegram.getFileDetails());

        TelegramFile audio = file("audio", 500);
        message.setAudio(audio);
        assertEquals("audio", telegram.getFileType());
        assertEquals(audio, telegram.getFileDetails());
        message.setDocument(file("document", 400));
        assertEquals("audio", telegram.getFileType());
        message.setAudio(null);
        assertEquals("doc", telegram.getFileType());
        message.setVideo(file("video", 300));
        assertEquals("doc", telegram.getFileType());
        message.setDocument(null);
        assertEquals("video", telegram.getFileType());
    }

    @Test
    void recognizesFileExtensionsOnlyAfterLastPathSeparator() {
        Telegram telegram = new Telegram();

        assertEquals(".pdf", telegram.getFileExtension("folder\\report.pdf"));
        assertEquals(".gz", telegram.getFileExtension("/folder/archive.tar.gz"));
        assertEquals("", telegram.getFileExtension("folder.with.dot/name"));
        assertEquals("", telegram.getFileExtension("README"));
    }

    @Test
    void sendsMessagesOnlyWithValidKeyAndNonemptyText() {
        Telegram telegram = configuredTelegram();
        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            requests.when(() -> ApiRequestHandler.postRequest(
                    "https://api.telegram.org/bot" + "x".repeat(21) + "/sendMessage",
                    Map.of("chat_id", "123", "text", "hello", "disable_web_page_preview", "true")))
                    .thenReturn("{\"ok\":true,\"result\":{}}");

            assertTrue(telegram.sendMessage(123, "hello"));
            assertFalse(telegram.sendMessage(123, ""));
            assertFalse(telegram.sendMessage(123, null));
            requests.verify(() -> ApiRequestHandler.postRequest(any(), any()), org.mockito.Mockito.times(1));
        }

        Telegram invalid = new Telegram();
        invalid.setApiKey("short");
        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            assertFalse(invalid.sendMessage(123, "hello"));
            requests.verifyNoInteractions();
        }

        Telegram unsuccessful = configuredTelegram();
        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            requests.when(() -> ApiRequestHandler.postRequest(any(), any())).thenReturn("{\"ok\":false}");
            assertFalse(unsuccessful.sendMessage("hello"));
        }
    }

    @Test
    void validatesWebhookSchemeAndApiReply() {
        Telegram telegram = configuredTelegram();
        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            requests.when(() -> ApiRequestHandler.getRequest(
                    "https://api.telegram.org/bot" + "x".repeat(21)
                            + "/setWebhook?url=https://example.test/hook"))
                    .thenReturn("{\"ok\":true,\"result\":true}");
            assertTrue(telegram.setWebhookUrl("https://example.test/hook"));
            assertFalse(telegram.setWebhookUrl("http://example.test/hook"));
            assertFalse(telegram.setWebhookUrl(null));
            requests.verify(() -> ApiRequestHandler.getRequest(contains("setWebhook")), org.mockito.Mockito.times(1));
        }

        Telegram unsuccessful = configuredTelegram();
        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            requests.when(() -> ApiRequestHandler.getRequest(any())).thenReturn("{\"ok\":true,\"result\":false}");
            assertFalse(unsuccessful.setWebhookUrl("https://example.test/hook"));
        }
    }

    @Test
    void downloadsFileUsingTelegramMetadataAndAppendsExtension() {
        Telegram telegram = configuredTelegram();
        TelegramFile document = file("file-id", 1);
        telegram.setUpdate(messageUpdateWithDocument(document));

        try (MockedStatic<ApiRequestHandler> requests = mockStatic(ApiRequestHandler.class)) {
            requests.when(() -> ApiRequestHandler.getRequest(contains("getFile?file_id=file-id")))
                    .thenReturn("{\"ok\":true,\"result\":{\"file_path\":\"documents/report.pdf\"}}");
            requests.when(() -> ApiRequestHandler.downloadFile(
                    "https://api.telegram.org/file/bot" + "x".repeat(21) + "/documents/report.pdf",
                    "download.pdf"))
                    .thenReturn(true);

            assertEquals("download.pdf", telegram.downloadFile("download"));
            requests.verify(() -> ApiRequestHandler.downloadFile(any(), any()));
        }

        Telegram noFile = configuredTelegram();
        assertNull(noFile.downloadFile("download"));
    }

    private static Telegram configuredTelegram() {
        Telegram telegram = new Telegram();
        telegram.setApiKey("bot" + "x".repeat(21));
        telegram.setAdminId(999);
        return telegram;
    }

    private static TelegramFile file(String id, int size) {
        TelegramFile file = new TelegramFile();
        file.setFile_id(id);
        file.setFile_size(size);
        return file;
    }

    private static TelegramUpdate messageUpdateWithDocument(TelegramFile document) {
        TelegramMessage message = new TelegramMessage();
        message.setDocument(document);
        TelegramUpdate update = new TelegramUpdate();
        update.setMessage(message);
        return update;
    }
}
