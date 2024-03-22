package com.kfels.shorturl.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class CommonUtils {

    static Logger log = Logger.getLogger(CommonUtils.class.getName());

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
}
