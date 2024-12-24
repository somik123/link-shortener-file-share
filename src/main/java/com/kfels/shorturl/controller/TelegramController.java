package com.kfels.shorturl.controller;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.telegram.Telegram;
import com.kfels.shorturl.utils.CommonUtils;

@RestController
@RequestMapping("/telegram")

public class TelegramController {

    @Autowired
    ShorturlService surlSvc;
    @Autowired
    UploadedFileService storageService;

    private static final Logger LOG = Logger.getLogger(TelegramController.class.getName());

    @PostMapping("/callback")
    public String receiveWebHook(@RequestBody String entity,
            @RequestHeader("x-telegram-bot-api-secret-token") String token) {
        LOG.info(entity);

        Telegram telegram = new Telegram(entity);

        if (token != null && token.length() > 0 && !telegram.isValidToken(token)) {
            LOG.warning("Invalid telegarm token. Restart app to reset token.");
            return "";
        }

        String replyToUser = "";

        int adminId = telegram.getAdminId();
        int chatId = telegram.getChatId();
        String message = telegram.getMessageText();

        if (telegram.isCommand()) {
            // Process commands
            if (message.equals("/url")) {
                replyToUser = System.getenv("SITE_FULL_URL");
            } else if (message.startsWith("/deleteSURL_")) {
                String[] parts = message.split("_");
                replyToUser = surlSvc.deleteShorturl(parts[1], parts[2]) ? "Success" : "Fail";
            } else if (message.startsWith("/deleteFile_")) {
                String[] parts = message.split("_");
                replyToUser = storageService.delete(parts[1], parts[2]) ? "Success" : "Fail";
            } else if (message.startsWith("/shorten_")) {
                String[] parts = message.split("_");
                UploadedFile file = storageService.getUploadFileFromDownloadKey(parts[1]);
                if (file != null) {
                    String longUrl = String.format("%sfile/%s/%s", System.getenv("SITE_FULL_URL"),
                            file.getDownloadKey(), file.getName());
                    generateShorturlAndNotify(longUrl, telegram);
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("List of available commands:\n");
                stringBuilder.append("/url - Reply with the url to the main website\n");
                stringBuilder.append("/deleteSURL_{shortUrlId}_{deleteKey} - Delete shorturl.\n");
                stringBuilder.append("/deleteFile_{fileId}_{deleteKey} - Delete file.\n");
                stringBuilder.append("/shorten_{fileId} - Generate a shorturl for uploaded file.\n");
                stringBuilder.append("/help - Shows the list of commands.\n");
                replyToUser = stringBuilder.toString();
            }
            telegram.sendMessage(chatId, replyToUser);
        } else if (telegram.getFileType() != null) {
            String filePath = String.format("tmp_uploads/%s", CommonUtils.randString(7, 3));
            filePath = telegram.downloadFile(filePath);
            if (filePath != null) {
                UploadedFile uploadedFile = storageService.saveFromTelegram(filePath, 1);
                if (uploadedFile != null) {
                    String url = String.format("%sfile/%s/%s", System.getenv("SITE_FULL_URL"),
                            uploadedFile.getDownloadKey(), uploadedFile.getName());
                    String deleteUrl = String.format("/deleteFile_%s_%s", uploadedFile.getDownloadKey(),
                            uploadedFile.getDeleteKey());

                    // Notify admin
                    if (chatId != adminId) {
                        String msg = String.format("New File uploaded: %s\nDelete: %s", url, deleteUrl);
                        CommonUtils.asynSendTelegramMessage(msg);
                    }
                    // Reply to user
                    replyToUser = String.format("Download: %s\nExpiry: 1 hour\nDelete: %s\nShorten: /shorten_%s", url,
                            deleteUrl, uploadedFile.getDownloadKey());
                } else {
                    replyToUser = "File upload failed";
                }
                telegram.sendMessage(chatId, replyToUser);
            }
        } else {
            if (CommonUtils.isValidURL(message)) {
                generateShorturlAndNotify(message, telegram);
            }
        }
        return "";
    }

    private void generateShorturlAndNotify(String longUrl, Telegram telegram) {
        int adminId = telegram.getAdminId();
        int chatId = telegram.getChatId();
        String replyToUser;

        Shorturl shorturl = surlSvc.getShorturlByLongurl(longUrl);
        if (shorturl != null) {
            replyToUser = String.format("ShortURL: %s%s\nEnabled: %s\nDelete: (hidden)", System.getenv("SITE_FULL_URL"),
                    shorturl.getSurl(), shorturl.getIsEnabled());
        } else {
            shorturl = surlSvc.generateShorturl(longUrl, "Telegram");

            if (shorturl != null) {
                String url = String.format("%s%s", System.getenv("SITE_FULL_URL"), shorturl.getSurl());
                String deleteUrl = String.format("/deleteSURL_%s_%s", shorturl.getSurl(), shorturl.getDeleteKey());

                // Notify admin
                if (chatId != adminId) {
                    String msg = String.format("New Short url: %s\nDelete: %s", url, deleteUrl);
                    CommonUtils.asynSendTelegramMessage(msg);
                }
                // Reply to user
                replyToUser = String.format("ShortURL: %s\nEnabled: %s\nDelete: %s", url, shorturl.getIsEnabled(),
                        deleteUrl);
            } else {
                replyToUser = "ShortURL generation failed";
            }
        }
        telegram.sendMessage(chatId, replyToUser);
    }

}
