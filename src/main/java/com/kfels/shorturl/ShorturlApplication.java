package com.kfels.shorturl;

import java.io.File;
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

		initializeDirectories();

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

	private static void initializeDirectories() {
		createDirectory("./data/db");
		createDirectory("./data/logs");
		createDirectory("./data/tmp_uploads");
		createDirectory("./data/uploads");
	}

	private static boolean createDirectory(String dir) {
		File file = new File(dir);
		if (file.exists()) {
			if (file.isDirectory())
				return true;
			else
				file.delete();
		}
		if (file.mkdirs())
			return true;
		else
			return false;
	}

}
