package com.sayless.task.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http.csrf(csrf->csrf.disable())
            .cors(cors->cors.configurationSource(req -> {
                CorsConfiguration c = new CorsConfiguration();
                c.setAllowedOrigins(Arrays.asList(allowedOrigins));
                c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                c.setAllowedHeaders(List.of("*"));
                c.setAllowCredentials(true);
                return c;

            })).formLogin(form->form.disable()).httpBasic(basic->basic.disable())
            //disable default spring security login and basic auth since we have our own
            //no auth required for spring health check, error handler and OPTIONS reqs from cors preflight
            .authorizeHttpRequests(auth->auth
            .requestMatchers("/actuator/health", "/error").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
            )
            //signaling touse custom filter instead of spring security chain's default one
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            
            return http.build();
    }
    
    
}
