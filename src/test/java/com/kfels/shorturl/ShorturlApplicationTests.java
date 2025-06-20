package com.kfels.shorturl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class ShorturlApplicationTests {

	@Test
	void exampleTest(@Autowired WebTestClient webClient) {
		webClient
			.get().uri("/")
			.exchange()
			.expectStatus().isOk();
	}

}
