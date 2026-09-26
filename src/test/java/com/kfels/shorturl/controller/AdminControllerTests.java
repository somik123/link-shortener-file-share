package com.kfels.shorturl.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ExtendedModelMap;

import com.kfels.shorturl.dto.FileDetailsDTO;
import com.kfels.shorturl.entity.Datalog;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;

class AdminControllerTests {

    private ShorturlService surlService;
    private UploadedFileService fileService;
    private AdminController controller;

    @BeforeEach
    void setUp() {
        surlService = mock(ShorturlService.class);
        fileService = mock(UploadedFileService.class);
        controller = new AdminController(surlService, fileService);
    }

    @Test
    void adminHomeIncludesUrlsEmptyMaximumAndAuthenticatedUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(surlService.getAllShorturls()).thenReturn(List.of());

        assertEquals("admin", controller.adminHome(model, new User("admin", "pw", List.of())));

        assertEquals(List.of(), model.get("surlList"));
        assertEquals(-1, model.get("max"));
        assertEquals("admin", model.get("user"));
    }

    @Test
    void urlTablesPaginateAndEmptyListHasNoItems() {
        List<Shorturl> urls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Shorturl url = new Shorturl();
            url.setSurl("url-" + i);
            urls.add(url);
        }
        when(surlService.getAllShorturls()).thenReturn(urls);

        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(CommonUtils::getPaginationSize).thenReturn(2);
            ExtendedModelMap model = new ExtendedModelMap();

            assertEquals("adminUrlTable", controller.adminUrlTable(model, 1));
            assertEquals(urls.subList(2, 4), model.get("surlList"));
            assertEquals(2, model.get("startIndex"));
            assertEquals(1, model.get("page"));
            assertEquals(3, model.get("maxPage"));
            assertEquals(1, model.get("max"));

            when(surlService.getAllShorturls()).thenReturn(List.of());
            assertEquals("adminUrlTable", controller.adminUrlTable(new ExtendedModelMap()));
        }
    }

    @Test
    void filePageAndFileTableRenderUserAndPaginatedFiles() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("adminFile", controller.adminFile(model, null));
        assertEquals("", model.get("user"));

        List<FileDetailsDTO> files = List.of(new FileDetailsDTO(), new FileDetailsDTO(), new FileDetailsDTO());
        when(fileService.getAllFileDetails()).thenReturn(files);
        try (MockedStatic<CommonUtils> commonUtils = mockStatic(CommonUtils.class)) {
            commonUtils.when(CommonUtils::getPaginationSize).thenReturn(2);
            ExtendedModelMap pageModel = new ExtendedModelMap();
            assertEquals("adminFileTable", controller.adminFileTable(pageModel, 1));
            assertEquals(files.subList(2, 3), pageModel.get("fileList"));
            assertEquals(2, pageModel.get("startIndex"));
            assertEquals(2, pageModel.get("maxPage"));
            assertEquals(0, pageModel.get("max"));

            when(fileService.getAllFileDetails()).thenReturn(List.of());
            assertEquals("adminFileTable", controller.adminFileTable(new ExtendedModelMap()));
        }
    }

    @Test
    void logPagesPopulateDetailsWhenRecordsExistAndRemainRenderableWhenMissing() {
        Shorturl shorturl = new Shorturl();
        shorturl.setSurl("alias");
        shorturl.setLogs(List.of(new Datalog()));
        when(surlService.getShorturlDetails("alias")).thenReturn(shorturl);

        ExtendedModelMap urlModel = new ExtendedModelMap();
        assertEquals("adminAllLogs", controller.getShorturlLogs(urlModel, "alias"));
        assertEquals("alias", urlModel.get("name"));
        assertSame(shorturl.getLogs(), urlModel.get("logs"));
        assertEquals(0, urlModel.get("max"));

        UploadedFile file = new UploadedFile();
        file.setName("my%20file.txt");
        file.setLogs(List.of(new Datalog()));
        when(fileService.getUploadFileFromDownloadKey("key")).thenReturn(file);
        ExtendedModelMap fileModel = new ExtendedModelMap();
        assertEquals("adminAllLogs", controller.getUploadedFileLogs(fileModel, "key"));
        assertEquals("my file.txt", fileModel.get("name"));
        assertSame(file.getLogs(), fileModel.get("logs"));

        when(surlService.getShorturlDetails("missing")).thenReturn(null);
        assertEquals("adminAllLogs", controller.getShorturlLogs(new ExtendedModelMap(), "missing"));
    }

    @Test
    void enableDisableHandlersToggleFoundRecordsAndIgnoreMissingOnes() {
        Shorturl shorturl = new Shorturl();
        shorturl.setSurl("alias");
        shorturl.setEnabled(false);
        when(surlService.getShorturlDetails("alias")).thenReturn(shorturl);
        assertEquals("redirect:/admin/", controller.enableDisableShorturl("alias"));
        assertEquals(true, shorturl.isEnabled());
        verify(surlService).save(shorturl);

        when(surlService.getShorturlDetails("missing")).thenReturn(null);
        assertEquals("redirect:/admin/", controller.enableDisableShorturl("missing"));
        verify(surlService, never()).save(null);

        UploadedFile file = new UploadedFile();
        file.setName("file.txt");
        file.setActive(false);
        when(fileService.getUploadFileFromDownloadKey("key", true)).thenReturn(file);
        assertEquals("redirect:/admin/file", controller.enableDisableUploadedFile("key"));
        assertEquals(true, file.isActive());
        verify(fileService).save(file);

        when(fileService.getUploadFileFromDownloadKey("missing", true)).thenReturn(null);
        assertEquals("redirect:/admin/file", controller.enableDisableUploadedFile("missing"));
    }
}
