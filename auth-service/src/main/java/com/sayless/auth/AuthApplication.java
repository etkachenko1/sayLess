package com.sayless.auth;
// Starts Spring Boot
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = { org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class }
)
public class AuthApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuthApplication.class, args);
  }
}