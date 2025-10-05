package com.kfels.shorturl.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
import com.kfels.shorturl.telegram.Telegram;

import jakarta.servlet.http.HttpServletRequest;

public class CommonUtils {

    private static final int[] ILLEGAL_CHARACTERS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

    private static Logger log = Logger.getLogger(CommonUtils.class.getName());

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

    public static int getShortUrlLength() {
        return getLengthFromString("SHORTURL_LENGTH", 5);
    }

    public static int getFileUrlLength() {
        return getLengthFromString("FILEURL_LENGTH", 10);
    }

    public static int getPaginationSize() {
        return getLengthFromString("PAGINATION_SIZE", 10);
    }

    public static String generateStringForShorturl(String surl) {
        int len = getShortUrlLength();
        int fileurl_len = getFileUrlLength();
        int surl_len = surl.length();
        if (surl != null && surl_len >= len && surl_len != fileurl_len) {
            return surl;
        }
        return randString(len, 3);
    }

    public static String generateStringForFileurl() {
        int len = getFileUrlLength();
        int shorturl_len = getShortUrlLength();
        if (len == shorturl_len) {
            len++; // Make sure fileurl is longer than shorturl
        }
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
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "X-Real-IP",
                "CF-Connecting-IP" // Cloudflare
        };

        for (String header : headers) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // Sometimes X-Forwarded-For contains multiple IPs
                String[] ips = ipList.split(",");
                for (String ip : ips) {
                    ip = ip.trim();
                    if (isPublicIp(ip)) {
                        return ip;
                    }
                }
            }
        }

        // fallback to request.getRemoteAddr()
        return request.getRemoteAddr();
    }

    public static String getClientIpAddressOld(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For may contain multiple IPs
            ip = ip.split(",")[0].trim();
            ip = normalizeIp(ip);
            if (isPublicIp(ip))
                return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            ip = normalizeIp(ip);
            if (isPublicIp(ip))
                return ip;
        }

        // Fallback
        ip = normalizeIp(request.getRemoteAddr());
        return ip;
    }

    private static String normalizeIp(String ip) {
        if (ip == null)
            return null;
        ip = ip.trim();

        // Strip [ ] around IPv6 if present
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        return ip;
    }

    private static boolean isPublicIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);

            if (inetAddress.isAnyLocalAddress() || // 0.0.0.0 or ::
                    inetAddress.isLoopbackAddress() || // 127.0.0.1 or ::1
                    inetAddress.isLinkLocalAddress() || // 169.254.x.x or fe80::/10
                    inetAddress.isSiteLocalAddress()) { // Private ranges: 10.x, 192.168.x, fc00::/7
                return false;
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
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

    public static String urlDecode(String encodedString) {
        try {
            return URLDecoder.decode(encodedString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logErrors(log, e);
            return "";
        }
    }
}
