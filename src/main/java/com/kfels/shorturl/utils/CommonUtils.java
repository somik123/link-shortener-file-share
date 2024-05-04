package com.kfels.shorturl.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import jakarta.servlet.http.HttpServletRequest;

public class CommonUtils {

    private static final int[] ILLEGAL_CHARACTERS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

    static Logger log = Logger.getLogger(CommonUtils.class.getName());

    public static Boolean isNameValid(String name) {
        for (char c : name.toCharArray()) {
            if (Arrays.binarySearch(ILLEGAL_CHARACTERS, c) >= 0) {
                return false;
            }
        }
        return true;
    }

    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); i++) {
            int c = (int) badFileName.charAt(i);
            if (Arrays.binarySearch(ILLEGAL_CHARACTERS, c) < 0) {
                cleanName.append((char) c);
            }
        }
        return cleanName.toString();
    }

    public static String formatSize(long v) {
        if (v < 1024)
            return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public static String randString() {
        int len = 0;
        String lengthString = System.getenv("SHORTURL_LENGTH");
        if (lengthString != null && lengthString.length() > 0) {
            len = Integer.parseInt(lengthString);
        }
        if (len <= 2) {
            len = 5;
        }
        return randString(len, 3);
    }

    public static String randString(int len) {
        return randString(len, 3);
    }

    public static String randString(int len, int mix) {
        String mix0 = "abcdefghijkmnpqstuvwxyz";
        String mix1 = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String mix2 = "23456789";

        String keyspace = mix0;
        if (mix > 0)
            keyspace += mix1;
        if (mix > 1)
            keyspace += mix2;

        String pieces = "";
        int max = keyspace.length() - 1;

        Random rand = new Random();
        for (int i = 0; i < len; i++) {
            int x = rand.nextInt(max);
            pieces += keyspace.charAt(x);
        }

        return pieces;
    }

    public static String isValidURL(String url) {
        if (url == null || url.length() == 0)
            return null;
        try {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8);
            log.info("Trying confirm this is a url: " + url);
            new URL(url);
            return url;
        } catch (Exception e) {
            log.warning("Invalid url: " + url);
            return null;
        }
    }

    public static byte[] generateQRCodeImage(String text, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); /* default = 4 */

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageConfig config = new MatrixToImageConfig(0xFF000002, 0xFFFFFFFF);

            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream, config);
            return outputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dataBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            BigInteger bigInt = new BigInteger(1, dataBytes);
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            int hash = data.hashCode();
            return String.valueOf(hash);
        }

    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            // As of https://en.wikipedia.org/wiki/X-Forwarded-For
            // The general format of the field is: X-Forwarded-For: client, proxy1, proxy2
            // ...
            // we only want the client
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
    }

    public static boolean sendTelegramMessage(String msg) {
        try {
            String apiKey = System.getenv("TELEGRAM_APIKEY");
            String chatId = System.getenv("TELEGRAM_CHATID");

            if (apiKey == null || apiKey.length() == 0 || chatId == null || chatId.length() == 0 || msg == null
                    || msg.length() == 0) {
                log.warning("Telegram message failed to send due to empty variables.");
                return false;
            }

            Map<String, String> postData = new HashMap<>();
            postData.put("chat_id", chatId);
            postData.put("text", msg);
            postData.put("disable_web_page_preview", "true");
            //postData.put("parse_mode","Markdown");

            String requestBody = getFormDataAsString(postData);
            System.out.println(requestBody);

            String url = "https://api.telegram.org/bot" + apiKey + "/sendMessage?" + requestBody;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());
            return (response.body().contains("{\"ok\":true)")) ? true : false;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            System.out.println(sStackTrace);
            log.warning(sStackTrace);
        }
        return false;
    }

    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder != null && formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }
}
