package com.kfels.shorturl.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CommonUtilsTests {

    @Test
    void validatesAndCleansNames() {
        assertTrue(CommonUtils.isNameValid("report 2026.txt"));
        assertFalse(CommonUtils.isNameValid("report/2026.txt"));
        assertEquals("report_2026.txt", CommonUtils.cleanFileName("report 2026.txt"));
        assertEquals("ab", CommonUtils.cleanFileName("a/b"));
    }

    @Test
    void formatsSizesAtUnitBoundaries() {
        assertEquals("1023 B", CommonUtils.formatSize(1023));
        assertEquals("1.0 KB", CommonUtils.formatSize(1024));
        assertEquals("1.0 MB", CommonUtils.formatSize(1024 * 1024));
    }

    @Test
    void generatesValidShortAndFileKeys() {
        int shortLength = CommonUtils.getShortUrlLength();
        int fileLength = CommonUtils.getFileUrlLength();
        String custom = "x".repeat(Math.max(shortLength, 3));
        if (custom.length() != fileLength) {
            assertEquals(custom, CommonUtils.generateStringForShorturl(custom));
        }
        String generated = CommonUtils.generateStringForShorturl("");
        assertEquals(shortLength, generated.length());
        assertEquals(fileLength == shortLength ? fileLength + 1 : fileLength,
                CommonUtils.generateStringForFileurl().length());
        assertEquals(7, CommonUtils.getLengthFromString("UNSET_TEST_KEY_LENGTH", 7));
        assertTrue(CommonUtils.getPaginationSize() > 2);
        assertEquals(12, CommonUtils.randString(12, 2).length());
        assertTrue(CommonUtils.randString(40, 0).matches("[a-z]+"));
    }

    @Test
    void validatesUrlsAndHashesAndDecodes() {
        assertTrue(CommonUtils.isValidURL("https://example.com/path"));
        assertFalse(CommonUtils.isValidURL("not a url"));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                CommonUtils.getHash("hello"));
        assertEquals("hello world", CommonUtils.urlDecode("hello%20world"));
    }

    @Test
    void selectsPublicForwardedAddressAndFallsBackToRemote() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 8.8.8.8");
        assertEquals("8.8.8.8", CommonUtils.getClientIpAddress(request));
        assertEquals("127.0.0.1", CommonUtils.getClientIpAddressOld(request));
        assertEquals("127.0.0.1", CommonUtils.getClientIpAddress(new MockHttpServletRequest()));
    }

    @Test
    void comparesSecretsAndCreatesQrCode() throws Exception {
        assertTrue(CommonUtils.secureEquals("secret", "secret"));
        assertFalse(CommonUtils.secureEquals("secret", "different"));
        assertFalse(CommonUtils.secureEquals(null, "secret"));
        assertFalse(CommonUtils.secureEquals("secret", null));
        byte[] png = CommonUtils.generateQRCodeImage("https://example.com", 120, 120);
        assertNotNull(png);
        assertEquals(120, ImageIO.read(new ByteArrayInputStream(png)).getWidth());
    }
}
