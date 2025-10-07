package com.sayless.task.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
//https://www.viralpatel.net/java-create-validate-jwt-token/
// helper for parsing and validating JWT tokens

@Component
public class JwtVerifier {
    private final Key key;

    //constructor to read from app.props or .env in future, converts it to a Key object using Keys.hmacShaKeyFor
    public JwtVerifier(@Value("${jwt.secret}") String secret){
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    
}
