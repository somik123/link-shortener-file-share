package com.kfels.shorturl.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

    @Autowired
    ShorturlService surlSvc;

    @Autowired
    UploadedFileService storagService;

    Logger log = Logger.getLogger(HomeController.class.getName());

    @GetMapping("/")
    public String siteHome() {
        return "home";
    }

    @GetMapping("/fileHome")
    public String fileHome() {
        return "fileHome";
    }

    @GetMapping("/login")
    public String adminLogin() {
        return "login";
    }

    @GetMapping("/admin")
    public RedirectView redirectToAdminController() {
        return new RedirectView("/admin/");
    }

    @GetMapping("/qr/{text}")
    public ResponseEntity<?> generateQRCode(@PathVariable String text) {
        if (text == null || text.length() == 0)
            return null;
        try {
            byte[] decode = Base64.getDecoder().decode(text);
            text = new String(decode, StandardCharsets.UTF_8);
            MediaType mediaType = MediaType.parseMediaType("image/png");
            byte[] image = CommonUtils.generateQRCodeImage(text, 150, 150);
            return ResponseEntity.ok().contentType(mediaType)
                    .body(image);
        } catch (Exception e) {
            return null;
        }

    }

    @GetMapping("/delete/{surl}/{deleteKey}")
    public String deleteShorturl(Model model, @PathVariable String surl, @PathVariable String deleteKey) {
        String status = surlSvc.deleteShorturl(surl, deleteKey) ? "yes" : "no";
        // return status ? new ResponseDTO("OK") : new ResponseDTO("FAIL");
        model.addAttribute("deleteAlert", "show");
        model.addAttribute("status", status);
        return "home";
    }

    @GetMapping("/deleteFile/{downloadKey}/{deleteKey}")
    public String deleteUploadedFile(Model model, @PathVariable String downloadKey, @PathVariable String deleteKey) {
        String status = storagService.delete(downloadKey, deleteKey) ? "yes" : "no";
        model.addAttribute("deleteAlert", "show");
        model.addAttribute("status", status);
        return "fileHome";
    }

    @GetMapping("/{surl}")
    public RedirectView redirectToShortUrl(@PathVariable String surl, HttpServletRequest request) {
        String creatorIp = request.getRemoteAddr();
        String browserHeaders = request.getHeader("User-Agent");
        String url = surlSvc.accessShorturl(surl, creatorIp, browserHeaders);
        if (url != null && url.length() > 0) {
            log.info("Redirecting [" + surl + "] to:" + url);
            return new RedirectView(url);
        } else {
            return new RedirectView("/");
        }
    }
}
