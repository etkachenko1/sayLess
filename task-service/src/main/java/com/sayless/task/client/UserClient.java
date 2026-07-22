package com.sayless.task.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
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
        if(userId == null) return null;
        try {
            Map <String, Object> user = restTemplate.getForObject(authUrl + "/users/" + userId, Map.class);
            if(user!=null && user.get("username")!=null)
                return user.get("username").toString();
        }
        catch (Exception e) {
            System.out.println("Failed to fetch username");
        }
        return "unknown";
    }
    
}
