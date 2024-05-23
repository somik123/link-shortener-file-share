package com.kfels.shorturl;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.kfels.shorturl.telegram.Telegram;

@SpringBootApplication
public class ShorturlApplication {

	private static final Logger LOG = Logger.getLogger(ShorturlApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(ShorturlApplication.class, args);

		Telegram telegram = new Telegram();
		String webhook = telegram.getSiteUrl() + "telegram/callback";
		LOG.info("Setting webhook to: " + webhook);
		if (telegram.setWebhookUrl(webhook))
			LOG.info("Webhook set successfully.");
		else
			LOG.info("Webhook was not set.");
	}

}
