package com.kfels.shorturl.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumMap;
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
import com.kfels.shorturl.telegram.Telegram;

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
            if (c == ' ') {
                cleanName.append('_');
            } else if (Arrays.binarySearch(ILLEGAL_CHARACTERS, c) < 0) {
                cleanName.append((char) c);
            }
        }
        return cleanName.toString();
    }

    public static String formatSize(long v) {
        if (v < 1024)
            return String.format("%d B", v);
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public static String generateStringForShorturl(String surl) {
        int len = getLengthFromString("SHORTURL_LENGTH", 5);
        if (surl != null && surl.length() >= len) {
            return surl;
        }
        return randString(len, 3);
    }

    public static String generateStringForFileurl() {
        int len = getLengthFromString("FILEURL_LENGTH", 10);
        return randString(len, 3);
    }

    public static int getLengthFromString(String envVarString, int defaultLen) {
        int len = 0;
        String lengthString = System.getenv(envVarString);
        if (lengthString != null && lengthString.length() > 0) {
            len = Integer.parseInt(lengthString);
        }
        if (len <= 2) {
            len = defaultLen;
        }
        return len;
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

    public static boolean isValidURL(String url) {
        try {
            log.info(String.format("Trying confirm this is a url: %s", url));
            new URI(url).toURL();
            return true;
        } catch (Exception e) {
            log.warning(String.format("Invalid url: %s", url));
            return false;
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

    // Send telegram message, but asynchronously
    public static void asynSendTelegramMessage(String msg) {
        new Thread(() -> new Telegram().sendMessage(msg)).start();
    }

    // Log errors
    public static void logErrors(Logger LOG, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString();
        System.out.println(sStackTrace);
        LOG.warning(sStackTrace);
    }

    public static String urlDecode(String encodedString){
        try{
            return URLDecoder.decode(encodedString, "UTF-8");
        }
        catch(UnsupportedEncodingException e){
            logErrors(log, e);
            return "";
        }
    }
}
