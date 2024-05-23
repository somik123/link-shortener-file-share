package com.kfels.shorturl.security;

import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private static Logger LOG = Logger.getLogger(WebSecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.disable()).csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/*", "/assets/**", "/api/**", "/qr/**", "/delete/**", "/deleteFile/**",
                                "/file/**", "/telegram/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin((form) -> form.loginPage("/login").permitAll())
                .logout((logout) -> logout.permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        String bucketUser = System.getenv("SHORTURL_USER");
        if (bucketUser == null || bucketUser.isEmpty())
            bucketUser = "user";
        String bucketPass = System.getenv("SHORTURL_PASS");
        if (bucketPass == null || bucketPass.isEmpty()) {
            bucketPass = "password";
        }

        String encodedPassword = passwordEncoder().encode(bucketPass);

        UserDetails user = User.builder()
                .username(bucketUser)
                .password(encodedPassword)
                .roles("USER")
                .build();

        String hidePass = System.getenv("SHORTURL_PASS_HIDE");
        if (hidePass == null || hidePass.isEmpty() || hidePass.toLowerCase().equals("no")
                || bucketUser.equals("user")) {
            LOG.info("\n");
            LOG.info("Admin username set to: " + bucketUser);
            LOG.info("Admin password set to: " + bucketPass);
            LOG.info("Encrypted password to: " + encodedPassword);
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
