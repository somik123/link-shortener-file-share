package com.kfels.shorturl.telegram.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class TelegramEntityTests {

    private final JsonMapper mapper = new JsonMapper();

    @Test
    void mapsNestedTelegramUpdateAndItsMessageEntities() throws Exception {
        String json = """
                {
                  "update_id": "u-1",
                  "message": {
                    "message_id": 7,
                    "from": {"id": 10, "is_bot": false, "username": "sender"},
                    "chat": {"id": 20, "type": "private", "username": "chat"},
                    "date": 100,
                    "text": "/start",
                    "entities": [{"offset": 0, "length": 6, "type": "bot_command"}],
                    "photo": [{"file_id": "photo-id", "file_size": 64, "width": 8, "height": 8}],
                    "document": {"file_name": "report.pdf", "mime_type": "application/pdf"},
                    "link_preview_options": {"url": "https://example.test"}
                  },
                  "edited_message": {"message_id": 8, "text": "edited"}
                }
                """;

        TelegramUpdate update = mapper.readValue(json, TelegramUpdate.class);

        assertEquals("u-1", update.getUpdate_id());
        assertEquals(7, update.getMessage().getMessage_id());
        assertEquals("sender", update.getMessage().getFrom().getUsername());
        assertEquals(20, update.getMessage().getChat().getId());
        assertFalse(update.getMessage().getFrom().isIs_bot());
        assertEquals("bot_command", update.getMessage().getEntities().get(0).getType());
        assertEquals(64, update.getMessage().getPhoto().get(0).getFile_size());
        assertEquals("report.pdf", update.getMessage().getDocument().getFile_name());
        assertEquals("https://example.test", update.getMessage().getLink_preview_options().getUrl());
        assertEquals("edited", update.getEdited_message().getText());

        TelegramUpdate roundTrip = mapper.readValue(mapper.writeValueAsString(update), TelegramUpdate.class);
        assertEquals(update, roundTrip);
    }

    @Test
    void mapsFileResponseAndNestedFileMetadata() throws Exception {
        TelegramResponse response = mapper.readValue(
                "{\"ok\":true,\"result\":{\"file_id\":\"id\",\"file_unique_id\":\"unique\","
                        + "\"file_path\":\"files/a.txt\",\"duration\":3,"
                        + "\"thumbnail\":{\"width\":2},\"thumb\":{\"height\":1}}}",
                TelegramResponse.class);

        assertTrue(response.isOk());
        assertEquals("id", response.getResult().getFile_id());
        assertEquals("unique", response.getResult().getFile_unique_id());
        assertEquals("files/a.txt", response.getResult().getFile_path());
        assertEquals(3, response.getResult().getDuration());
        assertEquals(2, response.getResult().getThumbnail().getWidth());
        assertEquals(1, response.getResult().getThumb().getHeight());
        assertEquals(response, mapper.readValue(mapper.writeValueAsString(response), TelegramResponse.class));
    }

    @Test
    void entityStringRepresentationsExposeTheirValues() {
        TelegramChat chat = new TelegramChat();
        chat.setId(1);
        chat.setIs_bot(true);
        chat.setUsername("user");
        TelegramEntities entity = new TelegramEntities();
        entity.setOffset(2);
        entity.setLength(4);
        entity.setType("mention");
        TelegramFile file = new TelegramFile();
        file.setFile_id("file");
        file.setFile_size(42);
        TelegramLinkPreview preview = new TelegramLinkPreview();
        preview.setUrl("https://example.test");
        TelegramMessage message = new TelegramMessage();
        message.setMessage_id(3);
        message.setText("message");
        message.setEntities(List.of(entity));
        TelegramUpdate update = new TelegramUpdate();
        update.setUpdate_id("update");
        update.setMessage(message);
        TelegramResponse response = new TelegramResponse();
        response.setOk(true);
        response.setResult(file);

        assertTrue(chat.toString().contains("username='user'"));
        assertTrue(entity.toString().contains("type='mention'"));
        assertTrue(file.toString().contains("file_size='42'"));
        assertTrue(preview.toString().contains("url='https://example.test'"));
        assertTrue(message.toString().contains("text='message'"));
        assertTrue(update.toString().contains("update_id='update'"));
        assertTrue(response.toString().contains("ok='true'"));
        assertNotNull(mapper.valueToTree(chat));
    }
}
