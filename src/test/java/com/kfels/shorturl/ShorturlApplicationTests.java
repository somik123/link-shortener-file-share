package com.kfels.shorturl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ShorturlApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void applicationContextLoads() {
		assertNotNull(context.getBean(ShorturlApplication.class));
	}

}
