/*
A JWT once-per-request filter involves the following steps:
Extract the token: The filter intercepts the incoming request and looks for the JWT, usually found in the Authorization header in the format Bearer <token>.
Validate the token: It validates the token's signature and expiration date, often with the help of a dedicated JWT utility class.
Load user details: If the token is valid, the filter extracts the user's identity from the token's claims and loads the corresponding user details (e.g., username, roles).
Set authentication context: The filter creates an authentication object from the user details and sets it in the SecurityContextHolder. This signals to the rest of the application that the user has been authenticated.
Chain the request: The request is then passed to the next filter in the security chain. If authentication was successful, subsequent components, like the AuthorizationFilter, can check for roles and permissions.
 */
// https://hackernoon.com/mastering-jwt-authentication-and-authorization-in-spring-boot-31

package com.sayless.notification.security;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{
    private final JwtVerifier verifier;
//JwtVerifier is injected to handle signature validation and claims parsing
    public JwtAuthFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        //skip filtering for server health check and browser CORS preflight request
        return path.equals("/actuator/health") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest req,
        @NonNull HttpServletResponse res,
        @NonNull FilterChain chain) throws ServletException, IOException {

        // look for the Authorization header in the incoming request
        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        //if header exists and starts with "Bearer ", extract the token part
        if(auth!= null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);//remove bearer prefix

            try {
                //validate and parse JWT using jwtVerifier
                Claims claims = verifier.parse(token);

                //extract the user id
                String userId = claims.getSubject();

                //build the authentication object with userid and token
                //users DO NOT HAVE any assigned permissions for now
                //POSSIBLE TO DO
                AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
                    @Override public Object getCredentials() {return token;}
                    @Override public Object getPrincipal() { return userId; }
                };
                //mark user as authenticated and put the Authentication into Spring Security context
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
            catch (Exception e) {
                System.out.println("Jwt verification failed: " + e.getMessage());
            }
        }
        //continue down the filter chain
        chain.doFilter(req,res);

        }
    
}
