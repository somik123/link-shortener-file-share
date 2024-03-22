package com.kfels.shorturl.controller;

import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import com.kfels.shorturl.entity.Datalog;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.service.ShorturlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    @Autowired
    ShorturlService surlSvc;

    Logger log = Logger.getLogger(AdminController.class.getName());

    @GetMapping("/")
    public String adminHome(Model model) {
        List<Shorturl> sList = surlSvc.getAllShorturls();
        model.addAttribute("sList", sList);
        model.addAttribute("max", sList.size() - 1);
        return "admin";
    }

    @GetMapping("/logs/{surl}")
    public String getShorturlLogs(Model model, @PathVariable String surl) {
        Shorturl shorturl = surlSvc.getShorturlDetails(surl);
        if (shorturl != null) {
            List<Datalog> datalogs = shorturl.getLogs();
            model.addAttribute("name", shorturl.getSurl());
            model.addAttribute("logs", datalogs);
            model.addAttribute("max", datalogs.size() - 1);
            log.info("Found info for: " + shorturl.getSurl());
        }
        return "showLogs";
    }
}
