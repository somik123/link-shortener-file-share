package com.kfels.shorturl.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.kfels.shorturl.service.UploadedFileService;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    UploadedFileService storageService;

    @Scheduled(cron = "@hourly")
    public void scheduledCronJobs() {
        storageService.cronJobs();
    }

}
