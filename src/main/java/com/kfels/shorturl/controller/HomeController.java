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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.kfels.shorturl.service.ShorturlService;
import com.kfels.shorturl.service.UploadedFileService;
import com.kfels.shorturl.telegram.Telegram;
import com.kfels.shorturl.utils.CommonUtils;
import com.kfels.shorturl.utils.Text2Image;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    ShorturlService surlSvc;

    @Autowired
    UploadedFileService storagService;

    private static final Logger LOG = Logger.getLogger(HomeController.class.getName());

    @GetMapping("/")
    // Display home page with url shortener
    public String siteHome() {
        return "home";
    }

    @GetMapping("/fileHome")
    // Display home page with file share
    public String fileHome(Model model) {
        String maxSize = System.getenv("UPLOADFILE_MAX_SIZE");
        if (maxSize != null && maxSize.length() > 0) {
            model.addAttribute("maxSize", maxSize);
        }
        return "fileHome";
    }

    @GetMapping("/login")
    // Admin login page
    public String adminLogin() {
        return "login";
    }

    // Change from /admin to /admin/ which is handled by admin controller
    @GetMapping("/admin")
    public RedirectView redirectToAdminController() {
        return new RedirectView("/admin/");
    }

    // Display captcha code and set captcha in session
    @GetMapping("/showImage")
    public ResponseEntity<?> generateCaptcha(HttpSession session) {
        // int codeNum = new Random().nextInt(11111, 99999);
        // String code = String.valueOf(codeNum);
        String code = CommonUtils.randString(6, 3);

        session.setAttribute("captchaCode", code);

        byte[] captchaImage = Text2Image.generate(code);
        MediaType mediaType = MediaType.parseMediaType("image/png");
        LOG.info(String.format("Generated captcha code: %s", code));
        return ResponseEntity.ok().contentType(mediaType)
                .body(captchaImage);
    }

    // Display QR code
    @GetMapping("/qr/{text}")
    public ResponseEntity<?> generateQRCode(@PathVariable String text) {
        if (text == null || text.length() == 0)
            return null;
        try {
            // text is base64 encoded to be transferred over the uri
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

    // Delete surl with surl & deleteKey
    @GetMapping("/delete/{surl}/{deleteKey}")
    public String deleteShorturl(Model model, @PathVariable String surl, @PathVariable String deleteKey) {
        String status = surlSvc.deleteShorturl(surl, deleteKey) ? "yes" : "no";
        // return status ? new ResponseDTO("OK") : new ResponseDTO("FAIL");
        model.addAttribute("deleteAlert", "show");
        model.addAttribute("status", status);
        return "home";
    }

    // Delete file with downloadKey & deleteKey
    @GetMapping("/deleteFile/{downloadKey}/{deleteKey}")
    public String deleteUploadedFile(Model model, @PathVariable String downloadKey, @PathVariable String deleteKey) {
        String status = storagService.delete(downloadKey, deleteKey) ? "yes" : "no";
        model.addAttribute("deleteAlert", "show");
        model.addAttribute("status", status);
        return "fileHome";
    }

    // Display contact form
    @GetMapping("/reach-out")
    public String showContactForm() {
        return "contactForm";
    }

    // Process contact form submitted data
    @PostMapping("/reach-out")
    public String submitContactForm(@RequestParam String name, @RequestParam String email, @RequestParam String link_id,
            @RequestParam String reason, @RequestParam String user_code, @RequestParam String message,
            Model model, HttpSession session) {

        String status = "no";
        String captchaCode = session.getAttribute("captchaCode").toString();
        LOG.info(String.format("Session captcha code: %s", captchaCode));
        LOG.info(String.format("User captcha code: %s", user_code));

        // Reset session captcha
        session.setAttribute("captchaCode", "");

        if (captchaCode != null && captchaCode.length() > 0) {

            if (captchaCode.equals(user_code) &&
                    name != null && name.length() > 0 &&
                    email != null && email.length() > 0 &&
                    reason != null && reason.length() > 0 &&
                    message != null && message.length() > 0) {

                // Send message via telegram
                String msg = String.format(
                        "New message received via contact form.\n\nName: %s\nEmail: %s\nLink: %s\nReason: %s\n\nMessage: %s\n",
                        name, email, link_id, reason, message);
                status = new Telegram().sendMessage(msg) ? "yes" : "no";
            }
        }
        model.addAttribute("status", status);
        return "contactForm";

    }

    // Redirect user by processing surl
    @GetMapping("/{surl}")
    public RedirectView redirectToShortUrl(@PathVariable String surl, HttpServletRequest request) {
        String creatorIp = request.getRemoteAddr();
        String browserHeaders = request.getHeader("User-Agent");
        String url = surlSvc.accessShorturl(surl, creatorIp, browserHeaders);
        if (url != null && url.length() > 0) {
            LOG.info(String.format("Redirecting [%s] to:%s", surl, url));
            return new RedirectView(url);
        } else {
            return new RedirectView("/");
        }
    }

}
