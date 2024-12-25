package com.kfels.shorturl.controller;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfels.shorturl.dto.RequestDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.dto.ShorturlDTO;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.utils.CommonUtils;
import com.kfels.shorturl.utils.DnsBlockList;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiController {

    @Autowired
    ShorturlService surlSvc;

    @Autowired
    UploadedFileService storageService;

    private static final Logger LOG = Logger.getLogger(ApiController.class.getName());

    @PostMapping("/shorten")
    public ResponseDTO shortenUrl(@RequestBody RequestDTO requestDTO, HttpServletRequest request) {
        // Ensure long url is valid
        String longUrl = null;
        if (CommonUtils.isValidURL(requestDTO.getUrl()))
            longUrl = requestDTO.getUrl();
        if (longUrl == null)
            return new ResponseDTO("FAIL", "", "Invalid url provided");

        // Check if the long url is already using a short url
        ShorturlDTO surlDto;
        Shorturl shorturl = surlSvc.getShorturlByLongurl(longUrl);
        if (shorturl != null) {
            surlDto = new ShorturlDTO(shorturl.getSurl(), shorturl.getIsEnabled());
            return new ResponseDTO("FAIL", surlDto, "Longurl already exists.");
        }

        // Ensure long url domain is not in block list
        if (DnsBlockList.checkBlockList(longUrl)) {
            return new ResponseDTO("FAIL", "", "Domain is blocked. Contact site owner for assistance.");
        }

        // Generate a new short url
        String creatorIp = CommonUtils.getClientIpAddress(request);
        shorturl = surlSvc.generateShorturl(longUrl, creatorIp, requestDTO.getSurl());
        if (shorturl == null) {
            return new ResponseDTO("FAIL", "", "Shorturl genertion failed.");
        } else {
            surlDto = new ShorturlDTO(shorturl.getSurl(), shorturl.getLongUrl(), shorturl.getDeleteKey(),
                    shorturl.getIsEnabled());

            // Notify admin
            String url = String.format("%s%s", System.getenv("SITE_FULL_URL"), shorturl.getSurl());
            String deleteUrl = String.format("/deleteSURL_%s_%s", shorturl.getSurl(), shorturl.getDeleteKey());
            String msg = String.format("New Short url: %s\nLong url: %s\nDelete: %s", url, longUrl, deleteUrl);
            CommonUtils.asynSendTelegramMessage(msg);

            LOG.info(msg);
            return new ResponseDTO("OK", surlDto, "");
        }
    }

    // Delete file with downloadKey & deleteKey
    @GetMapping("/deleteFile/{downloadKey}/{deleteKey}")
    public ResponseDTO deleteUploadedFileApi(@PathVariable String downloadKey, @PathVariable String deleteKey) {
        String status = storageService.delete(downloadKey, deleteKey) ? "OK" : "FAIL";
        return new ResponseDTO(status);
    }

    // Delete surl with surl & deleteKey
    @GetMapping("/delete/{surl}/{deleteKey}")
    public ResponseDTO deleteShorturlApi(@PathVariable String surl, @PathVariable String deleteKey) {
        String status = surlSvc.deleteShorturl(surl, deleteKey) ? "OK" : "FAIL";
        return new ResponseDTO(status);
    }

}
