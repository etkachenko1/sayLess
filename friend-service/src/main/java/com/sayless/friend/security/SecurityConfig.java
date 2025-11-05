package com.sayless.friend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; 
import org.springframework.security.web.SecurityFilterChain; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity; 
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; 
import org.springframework.web.cors.CorsConfiguration; 
import java.util.List;

/**
 * Love that you have this class for configs done very well. But again no lose strings , use configs from property file . Seprate logic from  data, properties always"
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http.csrf(csrf->csrf.disable())
            .cors(cors->cors.configurationSource(req -> {
                CorsConfiguration c = new CorsConfiguration();
                // this list cooule be populated here but instantiated somewhere else with no lose strings . Values come from property files or constants or enums
                c.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
                c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                c.setAllowedHeaders(List.of("*"));
                c.setAllowCredentials(true);
                return c;

            })).formLogin(form->form.disable()).httpBasic(basic->basic.disable())
            //disable default spring security login and basic auth since we have our own
            //no auth required for spring health check, error handler and OPTIONS reqs from cors preflight
            .authorizeHttpRequests(auth->auth
                    // This is perfect cuz you use lose trings with requestmatcher etc , or mappings . Good job
            .requestMatchers("/actuator/health", "/error").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
            )
            //signaling touse custom filter instead of spring security chain's default one
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            
            return http.build();
    }
    
    
}
