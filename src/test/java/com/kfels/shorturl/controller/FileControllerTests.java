package com.kfels.shorturl.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import com.kfels.shorturl.dto.FileDTO;
import com.kfels.shorturl.dto.FileLoadDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.ip2country.Ip2Country;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;

class FileControllerTests {

    private UploadedFileService fileService;
    private FileController controller;

    @BeforeEach
    void setUp() {
        fileService = mock(UploadedFileService.class);
        controller = new FileController(fileService);
    }

    @Test
    void uploadRejectsDisallowedCountryAndReportsStorageFailure() {
        MockMultipartFile upload = new MockMultipartFile("file", "sample.txt", "text/plain",
                "contents".getBytes(StandardCharsets.UTF_8));
        MockHttpServletRequest request = new MockHttpServletRequest();

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.1");
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.1")).thenReturn(false);

            var denied = controller.uploadFile(upload, 24, request);
            assertEquals(HttpStatus.UNAUTHORIZED, denied.getStatusCode());
            assertEquals("FAIL", ((ResponseDTO) denied.getBody()).getStatus());
            verify(fileService, never()).saveUploadedFile(upload, "192.0.2.1", 24);
        }

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.1");
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.1")).thenReturn(true);
            when(fileService.saveUploadedFile(upload, "192.0.2.1", 24)).thenReturn(null);

            var failed = controller.uploadFile(upload, 24, request);
            assertEquals(HttpStatus.OK, failed.getStatusCode());
            assertEquals("FAIL", ((ResponseDTO) failed.getBody()).getStatus());
            assertEquals("Upload failed.", ((ResponseDTO) failed.getBody()).getError());
        }
    }

    @Test
    void uploadReturnsSavedFileDetailsAndDoesNotRequireNotificationInfrastructure() {
        MockMultipartFile upload = new MockMultipartFile("file", "sample.txt", "text/plain",
                "contents".getBytes(StandardCharsets.UTF_8));
        MockHttpServletRequest request = new MockHttpServletRequest();
        FileDTO fileDTO = new FileDTO("sample.txt", "", "", "", "download", "delete", true);
        when(fileService.saveUploadedFile(upload, "192.0.2.2", 1)).thenReturn(fileDTO);

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class);
                MockedStatic<Ip2Country> country = mockStatic(Ip2Country.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.2");
            commonUtils.when(() -> CommonUtils.formatSize(upload.getSize())).thenReturn("8 B");
            country.when(() -> Ip2Country.isAccessAllowed("192.0.2.2")).thenReturn(true);

            var response = controller.uploadFile(upload, 1, request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("OK", ((ResponseDTO) response.getBody()).getStatus());
            assertEquals(fileDTO, ((ResponseDTO) response.getBody()).getContent());
            verify(fileService).saveUploadedFile(upload, "192.0.2.2", 1);
        }
    }

    @Test
    void downloadRejectsMissingResourceAndUsesSafeContentHeadersForSuccessfulFiles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "test browser");
        when(fileService.load("missing", "192.0.2.3", "test browser"))
                .thenReturn(new FileLoadDTO(new UploadedFile(), null));
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.3");
            var missing = controller.downloadFile("missing", "ignored.txt", request);
            assertEquals(HttpStatus.UNAUTHORIZED, missing.getStatusCode());
            assertEquals("FAIL", ((ResponseDTO) missing.getBody()).getStatus());
        }

        UploadedFile file = new UploadedFile();
        file.setName("report.pdf");
        Resource resource = new ByteArrayResource("pdf".getBytes(StandardCharsets.UTF_8));
        when(fileService.load("pdf-key", "192.0.2.3", "test browser"))
                .thenReturn(new FileLoadDTO(file, resource));
        when(fileService.getMimeType("pdf-key")).thenReturn("application/pdf");
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.3");
            var response = controller.downloadFile("pdf-key", "ignored.pdf", request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            assertEquals("inline; filename=\"report.pdf\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
            assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
            assertInstanceOf(org.springframework.core.io.InputStreamResource.class, response.getBody());
        }
    }

    @Test
    void unsafeMimeTypesAreAttachmentsAndDownloadErrorsAreMapped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "test browser");
        UploadedFile file = new UploadedFile();
        file.setName("payload.html");
        when(fileService.load("unsafe", "192.0.2.4", "test browser"))
                .thenReturn(new FileLoadDTO(file, new ByteArrayResource(new byte[] { 1 })));
        when(fileService.getMimeType("unsafe")).thenReturn("text/html");
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.4");
            var response = controller.downloadFile("unsafe", "ignored", request);
            assertEquals("attachment; filename=\"payload.html\"",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        }

        when(fileService.load("unknown-type", "192.0.2.4", "test browser"))
                .thenReturn(new FileLoadDTO(file, new ByteArrayResource(new byte[] { 1 })));
        when(fileService.getMimeType("unknown-type")).thenReturn(null);
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.4");
            var response = controller.downloadFile("unknown-type", "ignored", request);
            assertEquals("application/octet-stream", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        }

        Resource brokenResource = mock(Resource.class);
        when(brokenResource.getInputStream()).thenThrow(new IOException("read failure"));
        when(fileService.load("broken", "192.0.2.4", "test browser"))
                .thenReturn(new FileLoadDTO(file, brokenResource));
        when(fileService.getMimeType("broken")).thenReturn("application/pdf");
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.4");
            var failed = controller.downloadFile("broken", "ignored", request);
            assertEquals(HttpStatus.EXPECTATION_FAILED, failed.getStatusCode());
            assertEquals("Error while processing file.", ((ResponseDTO) failed.getBody()).getError());
        }

        when(fileService.load("unexpected", "192.0.2.4", "test browser"))
                .thenThrow(new IllegalStateException("storage unavailable"));
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(() -> CommonUtils.getClientIpAddress(request)).thenReturn("192.0.2.4");
            var failed = controller.downloadFile("unexpected", "ignored", request);
            assertEquals(HttpStatus.NOT_FOUND, failed.getStatusCode());
            assertEquals("Error while processing file.", ((ResponseDTO) failed.getBody()).getError());
        }
    }

    @Test
    void keyOnlyDownloadRedirectsToFileOrHomeAndCronDelegates() {
        UploadedFile file = new UploadedFile();
        file.setName("my file.txt");
        when(fileService.getUploadFileFromDownloadKey("known")).thenReturn(file);
        when(fileService.getUploadFileFromDownloadKey("missing")).thenReturn(null);
        when(fileService.cronJobs()).thenReturn(new ResponseDTO("OK"));

        RedirectView found = controller.downloadFile("known", new MockHttpServletRequest());
        assertEquals("known/my file.txt", found.getUrl());
        assertEquals("/", controller.downloadFile("missing", new MockHttpServletRequest()).getUrl());
        assertEquals("OK", controller.runCronJobs().getStatus());
        verify(fileService).cronJobs();
    }
}
