package com.sayless.auth.security;

//Utility service for handling JWT tokens.
// simple for now, will increase security later


//TO DO:
// https://medium.com/@tericcabrel/implement-jwt-authentication-in-a-spring-boot-3-application-5839e4fd8fac


import io.jsonwebtoken.*; //jjwt
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value; //to inject values from application.properties
import org.springframework.stereotype.Component; //tells the Spring this is a component it should manage
import java.util.Date;
import javax.crypto.SecretKey;

@Component //Spring Boot now will automatically create an object of this class and make it available when @Autowired is used.
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // generate token method
    public String generateToken(String userId) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}

//extract User Id from token 
public String getUserIdFromJwt(String token) {
    return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
}

//validate token method
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
        return true;
    }
    catch (JwtException e) {
        return false;
    }
}
    
}
