package com.kfels.shorturl.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.kfels.shorturl.service.UploadedFileService;

class SchedulerConfigTests {

    @Test
    void scheduledJobDelegatesToStorageService() {
        UploadedFileService storage = mock(UploadedFileService.class);
        new SchedulerConfig(storage).scheduledCronJobs();
        verify(storage).cronJobs();
    }
}
