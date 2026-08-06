package com.sayless.auth.security;
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

    public JwtAuthFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        //skip filtering for server health check, browser CORS preflight, and public auth routes
        return path.equals("/actuator/health")
            || "OPTIONS".equalsIgnoreCase(request.getMethod())
            || path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest req,
        @NonNull HttpServletResponse res,
        @NonNull FilterChain chain) throws ServletException, IOException {

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if(auth!= null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                Claims claims = verifier.parse(token);
                String userId = claims.getSubject();

                AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
                    @Override public Object getCredentials() {return token;}
                    @Override public Object getPrincipal() { return userId; }
                };
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
            catch (Exception e) {
                System.out.println("Jwt verification failed: " + e.getMessage());
            }
        }
        chain.doFilter(req,res);

        }

}
