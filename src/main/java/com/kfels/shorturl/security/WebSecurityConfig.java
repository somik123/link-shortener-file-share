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
@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.disable()).csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/*","/assets/**","/api/**","/qr/**","/delete/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin((form) -> form.loginPage("/login").permitAll())
                .logout((logout) -> logout.permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        Logger log = Logger.getLogger(WebSecurityConfig.class.getName());

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
            log.info("\n");
            log.info("Admin username set to: " + bucketUser);
            log.info("Admin password set to: " + bucketPass);
            log.info("Encrypted password to: " + encodedPassword);
            log.info("\n");
        } else {
            log.info("\n");
            log.info("Admin username and passwords are set to from ENV values.");
            log.info("\n");
        }

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
