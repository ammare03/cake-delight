package com.cakedelight.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Just the BCrypt bean — this service doesn't use spring-boot-starter-security
 * (see pom.xml comment). The gateway is the trust boundary (CLAUDE.md §4);
 * this service has no endpoints to lock down beyond what the controller does.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
