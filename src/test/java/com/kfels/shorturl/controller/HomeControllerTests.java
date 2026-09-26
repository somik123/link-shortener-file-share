package com.kfels.shorturl.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.view.RedirectView;

import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.telegram.Telegram;
import com.kfels.shorturl.utils.CommonUtils;

class HomeControllerTests {

    private ShorturlService surlService;
    private UploadedFileService fileService;
    private HomeController controller;

    @BeforeEach
    void setUp() {
        surlService = mock(ShorturlService.class);
        fileService = mock(UploadedFileService.class);
        controller = new HomeController(surlService, fileService);
    }

    @Test
    void homeAndLoginPagesPopulateAuthenticationAndFormState() {
        UserTestData user = new UserTestData();
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(CommonUtils::getShortUrlLength).thenReturn(7);
            commonUtils.when(CommonUtils::getFileUrlLength).thenReturn(12);
            ExtendedModelMap homeModel = new ExtendedModelMap();
            assertEquals("home", controller.siteHome(homeModel, user.user()));
            assertEquals("tester", homeModel.get("user"));
            assertEquals(false, homeModel.get("isFile"));
            assertEquals(6, homeModel.get("shorturl_len"));
            assertEquals(12, homeModel.get("fileurl_len"));
        }

        ExtendedModelMap fileModel = new ExtendedModelMap();
        assertEquals("fileHome", controller.fileHome(fileModel, null));
        assertEquals("", fileModel.get("user"));
        assertEquals(true, fileModel.get("isFile"));

        ExtendedModelMap loginModel = new ExtendedModelMap();
        assertEquals("login", controller.adminLogin(loginModel, null));
        assertEquals(false, loginModel.get("isFile"));
        assertEquals("contactForm", controller.showContactForm(new ExtendedModelMap(), null));
        assertEquals("forward:/assets/favicon/favicon.ico", controller.favicon());
        assertEquals("/admin/", controller.redirectToAdminController().getUrl());
    }

    @Test
    void captchaIsStoredInSessionAndReturnedAsPng() {
        MockHttpSession session = new MockHttpSession();

        var response = controller.generateCaptcha(session);

        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertEquals(6, session.getAttribute("captchaCode").toString().length());
        assertArrayEquals(new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47 },
                java.util.Arrays.copyOf((byte[]) response.getBody(), 4));
    }

    @Test
    void qrEndpointDecodesPayloadAndReturnsPngWhileInvalidPayloadIsRejected() {
        String payload = Base64.getEncoder().encodeToString("https://example.com".getBytes(StandardCharsets.UTF_8));
        byte[] image = new byte[] { 1, 2, 3 };
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.generateQRCodeImage("https://example.com", 150, 150)).thenReturn(image);
            var response = controller.generateQRCode(payload);
            assertEquals("image/png", response.getHeaders().getContentType().toString());
            assertArrayEquals(image, (byte[]) response.getBody());
            assertNull(controller.generateQRCode("not-base64"));
        }
    }

    @Test
    void deletePagesReflectSuccessAndFailure() {
        when(surlService.deleteShorturl("alias", "key")).thenReturn(true);
        when(fileService.delete("download", "key")).thenReturn(false);
        ExtendedModelMap urlModel = new ExtendedModelMap();
        assertEquals("home", controller.deleteShorturl(urlModel, "alias", "key"));
        assertEquals("show", urlModel.get("deleteAlert"));
        assertEquals("yes", urlModel.get("status"));

        ExtendedModelMap fileModel = new ExtendedModelMap();
        assertEquals("fileHome", controller.deleteUploadedFile(fileModel, "download", "key"));
        assertEquals("no", fileModel.get("status"));
    }

    @Test
    void contactSubmissionConsumesCaptchaAndSendsOnlyWhenCaptchaAndFieldsAreValid() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captchaCode", "ABC123");
        ExtendedModelMap model = new ExtendedModelMap();
        try (MockedConstruction<Telegram> telegrams = mockConstruction(Telegram.class, (telegram, context) ->
                when(telegram.sendMessage(anyString())).thenReturn(true))) {
            assertEquals("contactForm", controller.submitContactForm("Name", "name@example.com", "alias",
                    "question", "ABC123", "Message", model, session, null));
            assertEquals("yes", model.get("status"));
            assertEquals("", session.getAttribute("captchaCode"));
            verify(telegrams.constructed().get(0)).sendMessage(
                    org.mockito.ArgumentMatchers.contains("name@example.com"));
        }

        session.setAttribute("captchaCode", "ABC123");
        ExtendedModelMap invalidModel = new ExtendedModelMap();
        assertEquals("contactForm", controller.submitContactForm("Name", "name@example.com", "alias",
                "question", "wrong", "Message", invalidModel, session, null));
        assertEquals("no", invalidModel.get("status"));
    }

    @Test
    void shortUrlAndFileRedirectsHandleFoundMissingAndNormalAliases() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.8");
        request.addHeader("User-Agent", "browser");
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(CommonUtils::getShortUrlLength).thenReturn(6);
            commonUtils.when(CommonUtils::getFileUrlLength).thenReturn(10);
            when(surlService.accessShorturl("alias", "192.0.2.8", "browser"))
                    .thenReturn("https://destination.example/path");

            RedirectView shortUrl = controller.redirectToShortUrl("alias", request);
            assertEquals("https://destination.example/path", shortUrl.getUrl());

            when(surlService.accessShorturl("unknown", "192.0.2.8", "browser")).thenReturn(null);
            assertEquals("/", controller.redirectToShortUrl("unknown", request).getUrl());

            UploadedFile file = new UploadedFile();
            file.setName("document.txt");
            when(fileService.getUploadFileFromDownloadKey("1234567890")).thenReturn(file);
            String fileRedirect = controller.redirectToShortUrl("1234567890", request).getUrl();
            assertTrue(fileRedirect.endsWith("file/1234567890/document.txt"));
            when(fileService.getUploadFileFromDownloadKey("0000000000")).thenReturn(null);
            assertEquals("/", controller.redirectToShortUrl("0000000000", request).getUrl());
        }
    }

    private static class UserTestData {
        org.springframework.security.core.userdetails.User user() {
            return new org.springframework.security.core.userdetails.User("tester", "pw", java.util.List.of());
        }
    }
}
