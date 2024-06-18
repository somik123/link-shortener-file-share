package com.kfels.shorturl.controller;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfels.shorturl.dto.RequestDTO;
import com.kfels.shorturl.dto.ResponseDTO;
import com.kfels.shorturl.dto.ShorturlDTO;
import com.kfels.shorturl.entity.Shorturl;
import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.utils.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiController {

    @Autowired
    ShorturlService surlSvc;

    private static Logger LOG = Logger.getLogger(ApiController.class.getName());

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
        // Generate a new short url
        String creatorIp = CommonUtils.getClientIpAddress(request);
        shorturl = surlSvc.generateShorturl(longUrl, creatorIp, requestDTO.getSurl());
        if (shorturl == null) {
            return new ResponseDTO("FAIL", "", "Shorturl genertion failed.");
        } else {
            surlDto = new ShorturlDTO(shorturl.getSurl(), shorturl.getLongUrl(), shorturl.getDeleteKey(),
                    shorturl.getIsEnabled());

            // Notify admin
            String url = System.getenv("SITE_FULL_URL") + shorturl.getSurl();
            String deleteUrl = "/deleteSURL_" + shorturl.getSurl() + "_" + shorturl.getDeleteKey();
            String msg = "New Short url: " + url + "\n"
                    + "Long url:" + longUrl + "\n"
                    + "Delete: " + deleteUrl;
            CommonUtils.asynSendTelegramMessage(msg);

            LOG.info(msg);
            return new ResponseDTO("OK", surlDto, "");
        }
    }
}
