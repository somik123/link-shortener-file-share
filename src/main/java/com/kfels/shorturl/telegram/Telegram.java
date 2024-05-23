package com.kfels.shorturl.telegram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfels.shorturl.telegram.entity.TelegramFile;
import com.kfels.shorturl.telegram.entity.TelegramResponse;
import com.kfels.shorturl.telegram.entity.TelegramUpdate;
import com.kfels.shorturl.utils.CommonUtils;

public class Telegram {
    private static Logger LOG = Logger.getLogger(Telegram.class.getName());
    private String apiKey;
    private int adminId;
    private TelegramUpdate update;
    private String siteUrl;
    private String authKey;

    public Telegram() {
        setApiKeyFromEnv();
    }

    public Telegram(String apiKey, String adminId) {
        if (apiKey == null || apiKey.length() == 0 || adminId == null || adminId.length() == 0) {
            this.apiKey = apiKey;
            this.adminId = Integer.parseInt(adminId);
        }
    }

    public Telegram(String updateMsg) {
        setApiKeyFromEnv();
        setUpdateFromString(updateMsg);
    }

    public boolean setApiKeyFromEnv() {
        String apiKey = System.getenv("TELEGRAM_APIKEY");
        String adminId = System.getenv("TELEGRAM_ADMINID");
        String siteUrl = System.getenv("SITE_FULL_URL");
        String authKey = System.getenv("TELEGRAM_AUTHKEY");

        if (apiKey != null && apiKey.length() > 20 && adminId != null && adminId.length() > 0) {
            if (apiKey.substring(0, 3).equals("bot"))
                this.apiKey = apiKey;
            else
                this.apiKey = "bot" + apiKey;
            this.adminId = Integer.parseInt(adminId);
            this.siteUrl = siteUrl;
            this.authKey = authKey;
            return true;
        } else {
            LOG.warning("Telegram api key and admin chat id are not set.");
            return false;

        }
    }

    public boolean sendMessage(String message) {
        return sendMessage(this.adminId, message);
    }

    public boolean sendMessage(int chatId, String message) {

        String telegramApiUrl = "https://api.telegram.org/%s/sendMessage";
        if (message != null && message.length() > 0 && isApiKeyValid()) {
            telegramApiUrl = String.format(telegramApiUrl, this.apiKey);

            Map<String, String> postData = new HashMap<>();
            postData.put("chat_id", String.valueOf(chatId));
            postData.put("text", message);
            postData.put("disable_web_page_preview", "true");
            // postData.put("parse_mode","Markdown");

            String reply = ApiRequestHandler.postRequest(telegramApiUrl, postData);
            LOG.info(reply);
            if (reply.contains("\"ok\":true,"))
                return true;
        }
        return false;
    }

    public String downloadFile(String filePath) {
        try {
            if (getFileDetails() != null && isApiKeyValid()) {
                String fileId = getFileDetails().getFile_id();
                if (fileId != null && fileId.length() > 0) {
                    String url = String.format("https://api.telegram.org/%s/getFile?file_id=%s", apiKey, fileId);
                    String reply = ApiRequestHandler.getRequest(url);
                    System.out.println(url);
                    System.out.println(reply);

                    if (reply.contains("\"ok\":true,")) {
                        ObjectMapper mapper = new ObjectMapper();
                        TelegramResponse response = mapper.readValue(reply, TelegramResponse.class);
                        String telegramFilePath = response.getResult().getFile_path();

                        if (telegramFilePath != null && telegramFilePath.length() > 0) {

                            url = String.format("https://api.telegram.org/file/%s/%s", apiKey, telegramFilePath);
                            System.out.println(url);

                            String ext = getFileExtension(telegramFilePath);
                            if (ext.length() > 0)
                                filePath += ext;

                            System.out.println(filePath);
                            return ApiRequestHandler.downloadFile(url, filePath) ? filePath : null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
        }
        return null;
    }

    public boolean setWebhookUrl(String webhookUrl) {
        if (!isApiKeyValid())
            return false;

        String telegramApiUrl = "https://api.telegram.org/%s/setWebhook?url=%s";
        if (webhookUrl != null && webhookUrl.substring(0, 8).equals("https://")) {
            telegramApiUrl = String.format(telegramApiUrl, this.apiKey, webhookUrl);
            if (authKey != null && authKey.length() > 0)
                telegramApiUrl += String.format("&auth=%s", authKey);

            String reply = ApiRequestHandler.getRequest(telegramApiUrl);
            LOG.info(reply);
            if (reply.contains("\"ok\":true,\"result\":true"))
                return true;
        }
        return false;
    }

    public String getMessageText() {
        try {
            return update.getMessage().getText();
        } catch (NullPointerException e) {
        }
        return null;
    }

    public String getFileType() {
        try {
            if (update.getMessage().getAudio() != null)
                return "audio";
            else if (update.getMessage().getDocument() != null)
                return "doc";
            else if (update.getMessage().getVideo() != null)
                return "video";
            else if (update.getMessage().getPhoto() != null)
                return "image";
        } catch (NullPointerException e) {
        }
        return null;
    }

    public TelegramFile getFileDetails() {
        try {
            if (update.getMessage().getAudio() != null)
                return update.getMessage().getAudio();
            else if (update.getMessage().getDocument() != null)
                return update.getMessage().getDocument();
            else if (update.getMessage().getVideo() != null)
                return update.getMessage().getVideo();
            else if (update.getMessage().getPhoto() != null)
                return getImageDetails();
        } catch (NullPointerException e) {
        }
        return null;
    }

    public TelegramFile getImageDetails() {
        try {
            List<TelegramFile> tPhotoList = update.getMessage().getPhoto();
            int file_size = 0;
            int index = 0;
            for (int i = 0; i < tPhotoList.size(); i++) {
                int tmp = tPhotoList.get(i).getFile_size();
                if (tmp > file_size) {
                    file_size = tmp;
                    index = i;
                }
            }
            return tPhotoList.get(index);
        } catch (NullPointerException e) {
        }
        return null;
    }

    public boolean isCommand() {
        try {
            if (update.getMessage().getEntities().get(0).getType().equals("bot_command")) {
                return true;
            }

        } catch (NullPointerException e) {
        }
        return false;
    }

    public int getChatId() {
        try {
            return update.getMessage().getChat().getId();
        } catch (NullPointerException e) {
        }
        return 0;
    }

    public String getChatUsername() {
        try {
            return update.getMessage().getChat().getUsername();
        } catch (NullPointerException e) {
        }
        return null;
    }

    public int getMessageFromId() {
        try {
            return update.getMessage().getFrom().getId();
        } catch (NullPointerException e) {
        }
        return 0;
    }

    public String getMessageFromUsername() {
        try {
            return update.getMessage().getFrom().getUsername();
        } catch (NullPointerException e) {
        }
        return null;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getAdminId() {
        return this.adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public TelegramUpdate getUpdate() {
        return this.update;
    }

    public void setUpdate(TelegramUpdate update) {
        this.update = update;
    }

    public String getSiteUrl() {
        return this.siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public void setUpdateFromString(String updateMsg) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.update = mapper.readValue(updateMsg, TelegramUpdate.class);
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
        }
    }

    private boolean isApiKeyValid() {
        if (this.apiKey != null && this.apiKey.length() > 20)
            return true;
        else {
            System.out.println(apiKey);
            return false;
        }
    }

    public String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf(".");
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        System.out.println("i: " + i);
        System.out.println("p: " + p);
        if (i != -1 && i > p)
            return fileName.substring(i);
        return "";
    }
}
