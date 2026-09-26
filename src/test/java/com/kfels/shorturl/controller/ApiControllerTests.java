package com.kfels.shorturl.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import com.kfels.shorturl.dto.RequestDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.dto.ShorturlDTO;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.ip2country.Ip2Country;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;
import com.kfels.shorturl.utils.DnsBlockList;

class ApiControllerTests {

    private ShorturlService surlService;
    private UploadedFileService fileService;
    private ApiController controller;

    @BeforeEach
    void setUp() {
        surlService = mock(ShorturlService.class);
        fileService = mock(UploadedFileService.class);
        controller = new ApiController(surlService, fileService);
    }

    @Test
    void shortenRejectsInvalidUrlWithoutCallingServices() {
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setUrl("not a URL");
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL("not a URL")).thenReturn(false);
            ResponseDTO result = controller.shortenUrl(requestDTO, new MockHttpServletRequest());

            assertEquals("FAIL", result.getStatus());
            assertEquals("Invalid url provided", result.getError());
            verify(surlService, never()).getShorturlByLongurl("not a URL");
        }
    }

    @Test
    void shortenReturnsExistingAliasAndRejectsBlockedDomain() {
        String longUrl = "https://example.com/path";
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setUrl(longUrl);
        Shorturl existing = new Shorturl();
        existing.setSurl("existing");
        existing.setEnabled(true);
        when(surlService.getShorturlByLongurl(longUrl)).thenReturn(existing);

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL(longUrl)).thenReturn(true);
            ResponseDTO duplicate = controller.shortenUrl(requestDTO, new MockHttpServletRequest());
            assertEquals("FAIL", duplicate.getStatus());
            assertEquals("Longurl already exists.", duplicate.getError());
            assertNotNull(duplicate.getContent());
            verify(surlService, never()).generateShorturl(longUrl, "192.0.2.20", null);
        }

        when(surlService.getShorturlByLongurl(longUrl)).thenReturn(null);
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<DnsBlockList> blockList = mockStatic(DnsBlockList.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL(longUrl)).thenReturn(true);
            blockList.when(() -> DnsBlockList.checkBlockList(longUrl)).thenReturn(true);

            ResponseDTO blocked = controller.shortenUrl(requestDTO, new MockHttpServletRequest());
            assertEquals("FAIL", blocked.getStatus());
            assertEquals("Domain is blocked. Contact site owner for assistance.", blocked.getError());
            verify(surlService, never()).generateShorturl(longUrl, "192.0.2.20", null);
        }
    }

    @Test
    void shortenDeniesRestrictedCountryAndReportsGenerationFailure() {
        String longUrl = "https://example.com/new";
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setUrl(longUrl);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(surlService.getShorturlByLongurl(longUrl)).thenReturn(null);
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<DnsBlockList> blockList = mockStatic(DnsBlockList.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL(longUrl)).thenReturn(true);
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.5");
            blockList.when(() -> DnsBlockList.checkBlockList(longUrl)).thenReturn(false);
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.5")).thenReturn(false);

            ResponseDTO denied = controller.shortenUrl(requestDTO, request);
            assertEquals("FAIL", denied.getStatus());
            assertEquals("Access denied for your country.", denied.getError());
            verify(surlService, never()).generateShorturl(longUrl, "192.0.2.5", null);
        }

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<DnsBlockList> blockList = mockStatic(DnsBlockList.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL(longUrl)).thenReturn(true);
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.5");
            blockList.when(() -> DnsBlockList.checkBlockList(longUrl)).thenReturn(false);
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.5")).thenReturn(true);
            when(surlService.generateShorturl(longUrl, "192.0.2.5", null)).thenReturn(null);

            ResponseDTO failed = controller.shortenUrl(requestDTO, request);
            assertEquals("FAIL", failed.getStatus());
            assertEquals("Shorturl genertion failed.", failed.getError());
        }
    }

    @Test
    void shortenCreatesAndReturnsNewShortUrl() {
        String siteUrl = System.getenv("SITE_FULL_URL");
        Assumptions.assumeTrue(siteUrl != null && !siteUrl.isBlank(),
                "SITE_FULL_URL is required by ApiController to build the generated URL");

        String longUrl = "https://example.com/new";
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setUrl(longUrl);
        requestDTO.setSurl("custom-alias");
        MockHttpServletRequest request = new MockHttpServletRequest();
        Shorturl generated = new Shorturl();
        generated.setSurl("custom-alias");
        generated.setLongUrl(longUrl);
        generated.setDeleteKey("delete-secret");
        generated.setEnabled(true);

        when(surlService.getShorturlByLongurl(longUrl)).thenReturn(null);
        when(surlService.generateShorturl(longUrl, "192.0.2.9", "custom-alias")).thenReturn(generated);
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<DnsBlockList> blockList = mockStatic(DnsBlockList.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.isValidURL(longUrl)).thenReturn(true);
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.9");
            blockList.when(() -> DnsBlockList.checkBlockList(longUrl)).thenReturn(false);
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.9")).thenReturn(true);
            commonUtils.when(() -> CommonUtils.asynSendTelegramMessage(org.mockito.ArgumentMatchers.anyString()))
                    .thenAnswer(invocation -> null);

            ResponseDTO result = controller.shortenUrl(requestDTO, request);

            assertEquals("OK", result.getStatus());
            assertEquals("", result.getError());
            ShorturlDTO content = (ShorturlDTO) result.getContent();
            assertEquals("custom-alias", content.getSurl());
            assertEquals(longUrl, content.getLongUrl());
            assertEquals("delete-secret", content.getDeleteKey());
            assertTrue(content.isEnabled());
            verify(surlService).generateShorturl(longUrl, "192.0.2.9", "custom-alias");
            verify(surlService, never()).save(generated);
        }
    }

    @Test
    void deleteAndCronEndpointsReturnServiceOutcomes() {
        when(surlService.deleteShorturl("alias", "secret")).thenReturn(true);
        when(fileService.delete("download", "secret")).thenReturn(false);
        when(fileService.cronJobs()).thenReturn(new ResponseDTO("OK"));

        assertEquals("OK", controller.deleteShorturlApi("alias", "secret").getStatus());
        assertEquals("FAIL", controller.deleteUploadedFileApi("download", "secret").getStatus());
        assertEquals("OK", controller.scheduledCronJobs().getStatus());
        verify(surlService).deleteShorturl("alias", "secret");
        verify(fileService).delete("download", "secret");
        verify(fileService).cronJobs();
    }

    @Test
    void ipEndpointUsesResolvedClientAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("203.0.113.7");
            assertEquals("203.0.113.7", controller.returnClientIp(request));
        }
    }
}
