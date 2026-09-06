package com.kfels.shorturl.config;

import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private static final Logger LOG = Logger.getLogger(WebSecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.disable())
                // The public API endpoints (/api/**, /file/upload, /telegram/**) are called by
                // anonymous, non-browser/cross-origin clients and don't rely on cookies for
                // authorization (they use random delete/download keys instead), so they are
                // exempted from CSRF. All authenticated /admin/** actions remain protected.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**", "/file/upload", "/telegram/**", "/h2-*/**"))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/*", "/assets/**", "/adminAssets/**", "/api/**", "/qr/**", "/showImage",
                                "/delete/**", "/deleteFile/**", "/file/**", "/telegram/**", "/reach-out")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin((form) -> form.loginPage("/login").permitAll())
                .logout((logout) -> logout.permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        String shortUrlUser = System.getenv("SHORTURL_USER");
        if (shortUrlUser == null || shortUrlUser.isEmpty())
            shortUrlUser = "user";
        String shortUrlPass = System.getenv("SHORTURL_PASS");
        if (shortUrlPass == null || shortUrlPass.isEmpty()) {
            shortUrlPass = "password";
        }

        String encodedPassword = passwordEncoder().encode(shortUrlPass);

        UserDetails user = User.builder()
                .username(shortUrlUser)
                .password(encodedPassword)
                .roles("USER")
                .build();

        String hidePass = System.getenv("SHORTURL_PASS_HIDE");
        if (hidePass == null || hidePass.isEmpty() || hidePass.toLowerCase().equals("no")
                || shortUrlUser.equals("user")) {
            LOG.info("\n");
            LOG.info(String.format("Admin username set to: %s", shortUrlUser));
            LOG.info(String.format("Admin password set to: %s", shortUrlPass));
            LOG.info(String.format("Encrypted password to: %s", encodedPassword));
            LOG.info("\n");
        } else {
            LOG.info("\n");
            LOG.info("Admin username and passwords are set to from ENV values.");
            LOG.info("\n");
        }

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
