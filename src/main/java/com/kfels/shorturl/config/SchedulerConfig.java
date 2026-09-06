package com.kfels.shorturl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.kfels.shorturl.service.UploadedFileService;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    private final UploadedFileService storageService;

    public SchedulerConfig(UploadedFileService storageService) {
        this.storageService = storageService;
    }

    @Scheduled(cron = "@hourly")
    public void scheduledCronJobs() {
        storageService.cronJobs();
    }

}
