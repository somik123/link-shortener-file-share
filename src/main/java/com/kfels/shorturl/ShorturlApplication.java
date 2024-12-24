package com.kfels.shorturl;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.kfels.shorturl.telegram.Telegram;
import com.kfels.shorturl.utils.DnsBlockList;

@SpringBootApplication
public class ShorturlApplication {

	private static final Logger LOG = Logger.getLogger(ShorturlApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(ShorturlApplication.class, args);

		// Setup telegram webhook
		Telegram telegram = new Telegram();
		String webhook = String.format("%stelegram/callback", telegram.getSiteUrl());
		if (telegram.setWebhookUrl(webhook))
			LOG.info("Webhook set successfully.");
		else
			LOG.info("Webhook was not set.");
		
		// Update DNS blocklist domains
		DnsBlockList.updateBlockList();
	}

}
