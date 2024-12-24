package com.kfels.shorturl.controller;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kfels.shorturl.dto.FileDetailsDTO;
import com.kfels.shorturl.entity.Datalog;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.entity.UploadedFile;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;

@Controller
@RequestMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    @Autowired
    ShorturlService surlSvc;

    @Autowired
    UploadedFileService storageService;

    private static final Logger LOG = Logger.getLogger(AdminController.class.getName());

    @GetMapping("/")
    public String adminHome(Model model) {
        List<Shorturl> surlList = surlSvc.getAllShorturls();
        model.addAttribute("surlList", surlList);
        model.addAttribute("max", surlList.size() - 1);
        return "admin";
    }

    @GetMapping("/file")
    public String adminFile(Model model) {
        List<FileDetailsDTO> fileList = storageService.getAllFileDetails();
        model.addAttribute("fileList", fileList);
        model.addAttribute("max", fileList.size() - 1);
        return "adminFile";
    }

    @GetMapping("/logs/{surl}")
    public String getShorturlLogs(Model model, @PathVariable String surl) {
        Shorturl shorturl = surlSvc.getShorturlDetails(surl);
        if (shorturl != null) {
            List<Datalog> datalogs = shorturl.getLogs();
            model.addAttribute("name", shorturl.getSurl());
            model.addAttribute("logs", datalogs);
            model.addAttribute("max", datalogs.size() - 1);
            LOG.info(String.format("Found info for: %s", shorturl.getSurl()));
        }
        return "showLogs";
    }

    @GetMapping("/fileLogs/{downloadKey}")
    public String getUploadedFileLogs(Model model, @PathVariable String downloadKey) {
        UploadedFile file = storageService.getUploadFileFromDownloadKey(downloadKey);
        if (file != null) {
            List<Datalog> datalogs = file.getLogs();
            model.addAttribute("name", file.getName());
            model.addAttribute("logs", datalogs);
            model.addAttribute("max", datalogs.size() - 1);
            LOG.info(String.format("Found info for: %s", file.getName()));
        }
        return "showLogs";
    }
}
