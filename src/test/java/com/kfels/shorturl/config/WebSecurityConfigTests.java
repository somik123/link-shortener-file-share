package com.kfels.shorturl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class WebSecurityConfigTests {

    @Test
    void configuredUserCanAuthenticateWithConfiguredPassword() {
        WebSecurityConfig config = new WebSecurityConfig();
        String username = System.getenv("SHORTURL_USER");
        if (username == null || username.isEmpty()) {
            username = "user";
        }
        String password = System.getenv("SHORTURL_PASS");
        if (password == null || password.isEmpty()) {
            password = "password";
        }

        UserDetails user = config.userDetailsService().loadUserByUsername(username);
        assertEquals(username, user.getUsername());
        assertTrue(config.passwordEncoder().matches(password, user.getPassword()));
    }
}
