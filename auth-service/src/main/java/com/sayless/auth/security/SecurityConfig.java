package com.sayless.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF config
            .csrf(csrf -> csrf.disable()) // disable entirely for APIs
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll() // allow public access to /auth/*
                .anyRequest().authenticated()
            )
            // Disable default login form
            .formLogin(form -> form.disable())         // disable default login page
            .httpBasic(basic -> basic.disable());      // disable BasicAuth

        return http.build();
    }
}
