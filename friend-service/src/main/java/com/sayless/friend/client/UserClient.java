package com.sayless.friend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class UserClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String authUrl;

    public UserClient(@Value("${auth.service.url}") String authUrl) {
        this.authUrl = authUrl;
    }

    @SuppressWarnings("unchecked")
    public String getUsername(String userId) {
        if (userId == null) return null;
        try {
            URI uri = UriComponentsBuilder.fromUriString(authUrl)
                .path("/users/{id}")
                .buildAndExpand(userId)
                .toUri();
            Map<String, Object> user = restTemplate.getForObject(uri, Map.class);
            if (user != null && user.get("username") != null)
                return user.get("username").toString();
        } catch (Exception e) {
            System.out.println("Failed to fetch username for " + userId + ": " + e.getMessage());
        }
        return "unknown";
    }

    public Object[] searchUsers(String username) {
        URI uri = UriComponentsBuilder.fromUriString(authUrl)
            .path("/users/search")
            .queryParam("username", username)
            .build()
            .toUri();
        return restTemplate.getForObject(uri, Object[].class);
    }
}
