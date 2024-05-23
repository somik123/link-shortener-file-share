package com.kfels.shorturl.telegram;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;

@RestController
@RequestMapping("/telegram")

public class TelegramController {

    @Autowired
    ShorturlService surlSvc;
    @Autowired
    UploadedFileService storageService;

    private static Logger LOG = Logger.getLogger(TelegramController.class.getName());

    @PostMapping("/callback")
    public String receiveWebHook(@RequestBody String entity) {
        LOG.info(entity);

        Telegram telegram = new Telegram(entity);
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
                replyToUser += surlSvc.deleteShorturl(parts[1], parts[2]) ? "Success" : "Fail";
            } else if (message.startsWith("/deleteFile_")) {
                String[] parts = message.split("_");
                replyToUser = storageService.delete(parts[1], parts[2]) ? "Success" : "Fail";
            } else if (message.startsWith("/shorten_")) {
                String[] parts = message.split("_");
                UploadedFile file = storageService.getUploadFileFromDownloadKey(parts[1]);
                if (file != null) {
                    String longUrl = System.getenv("SITE_FULL_URL") + "file/" + file.getDownloadKey() + "/"
                            + file.getName();
                    generateShorturlAndNotify(longUrl, telegram);
                }
            } else {
                replyToUser = "List of available commands:\n"
                        + "/url - Reply with the url to the main website\n"
                        + "/deleteSURL_{shortUrlId}_{deleteKey} - Delete shorturl.\n"
                        + "/deleteFile_{fileId}_{deleteKey} - Delete file.\n"
                        + "/shorten_{fileId} - Generate a shorturl for uploaded file.\n"
                        + "/help - Shows the list of commands.\n";
            }
            telegram.sendMessage(chatId, replyToUser);
        } else if (telegram.getFileType() != null) {
            String filePath = "tmp_uploads/" + CommonUtils.randString(7, 3);
            filePath = telegram.downloadFile(filePath);
            if (filePath != null) {
                UploadedFile uploadedFile = storageService.saveFromTelegram(filePath, 1);
                if (uploadedFile != null) {
                    String url = System.getenv("SITE_FULL_URL") + "file/" + uploadedFile.getDownloadKey() + "/"
                            + uploadedFile.getName();
                    String deleteUrl = "/deleteFile_" + uploadedFile.getDownloadKey() + "_"
                            + uploadedFile.getDeleteKey();

                    // Notify admin
                    if (chatId != adminId) {
                        String msg = "New File uploaded: " + url + "\n" +
                                "Delete: " + deleteUrl;
                        CommonUtils.asynSendTelegramMessage(msg);
                    }
                    // Reply to user
                    replyToUser = "Download: " + url + "\n"
                            + "Expiry: 1 hour\n"
                            + "Delete: " + deleteUrl + "\n"
                            + "Shorten: /shorten_" + uploadedFile.getDownloadKey();
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
            replyToUser = "ShortURL: " + System.getenv("SITE_FULL_URL") + shorturl.getSurl() + "\n"
                    + "Enabled: " + shorturl.getIsEnabled() + "\n"
                    + "Delete: (hidden)";
        } else {
            shorturl = surlSvc.generateShorturl(longUrl, "Telegram");

            if (shorturl != null) {
                String url = System.getenv("SITE_FULL_URL") + shorturl.getSurl();
                String deleteUrl = "/deleteSURL_" + shorturl.getSurl() + "_" + shorturl.getDeleteKey();

                // Notify admin
                if (chatId != adminId) {
                    String msg = "New Short url: " + url + "\n"
                            + "Delete: " + deleteUrl;
                    CommonUtils.asynSendTelegramMessage(msg);
                }
                // Reply to user
                replyToUser = "ShortURL: " + url + "\n"
                        + "Enabled: " + shorturl.getIsEnabled() + "\n"
                        + "Delete: " + deleteUrl;
            } else {
                replyToUser = "ShortURL generation failed";
            }
        }
        telegram.sendMessage(chatId, replyToUser);
    }

}
