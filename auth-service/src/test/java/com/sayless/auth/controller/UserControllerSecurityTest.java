package com.sayless.auth.controller;

import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;
import com.sayless.auth.security.JwtAuthFilter;
import com.sayless.auth.security.JwtVerifier;
import com.sayless.auth.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression coverage for the auth-service IDOR fix: PUT /users/{id} where any caller could overwrite
// any other user's profile was replaced with PUT /users/me, which derives the target user solely
// from the JWT subject. These tests assert the endpoint requires a valid token, and that a valid
// token only ever updates the token owner's own record.
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtVerifier.class})
@TestPropertySource(properties = {
    "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789",
    "cors.allowed-origins=http://localhost:5173"
})
class UserControllerSecurityTest {

    private static final String SECRET = "test-only-secret-not-used-anywhere-else-0123456789";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private String tokenFor(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void putUsersMe_withoutToken_isRejected() throws Exception {
        mockMvc.perform(put("/users/me")
                        .contentType("application/json")
                        .content("{\"bio\":\"hi\"}"))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any());
    }

    @Test
    void putUsersMe_withValidToken_onlyUpdatesTheTokenOwnersOwnRecord() throws Exception {
        User caller = new User("alice", "alice@example.com", "hashed");
        caller.setId("caller-id");
        when(userRepository.findById("caller-id")).thenReturn(Optional.of(caller));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + tokenFor("caller-id"))
                        .contentType("application/json")
                        .content("{\"bio\":\"new bio\"}"))
                .andExpect(status().isOk());

        // the handler never takes an id from the request at all, it can only ever
        // look up and save the id embedded in the caller's own token
        verify(userRepository).findById("caller-id");
        verify(userRepository, never()).findById(argThat(id -> !"caller-id".equals(id)));
    }

    @Test
    void putUsersMe_bioOverMaxLength_isRejected() throws Exception {
        User caller = new User("alice", "alice@example.com", "hashed");
        caller.setId("caller-id");
        when(userRepository.findById("caller-id")).thenReturn(Optional.of(caller));

        String tooLong = "x".repeat(501);
        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + tokenFor("caller-id"))
                        .contentType("application/json")
                        .content("{\"bio\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void putUsersMe_nonHttpsProfilePic_isRejected() throws Exception {
        User caller = new User("alice", "alice@example.com", "hashed");
        caller.setId("caller-id");
        when(userRepository.findById("caller-id")).thenReturn(Optional.of(caller));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + tokenFor("caller-id"))
                        .contentType("application/json")
                        .content("{\"profilePic\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_forSomeoneElse_returnsOnlyIdAndUsername() throws Exception {
        User target = new User("bob", "bob@example.com", "hashed");
        target.setId("bob-id");
        when(userRepository.findById("bob-id")).thenReturn(Optional.of(target));

        mockMvc.perform(get("/users/bob-id")
                        .header("Authorization", "Bearer " + tokenFor("someone-else-id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void getUserById_withNoToken_returnsOnlyIdAndUsername() throws Exception {
        User target = new User("bob", "bob@example.com", "hashed");
        target.setId("bob-id");
        when(userRepository.findById("bob-id")).thenReturn(Optional.of(target));

        mockMvc.perform(get("/users/bob-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void getUserById_forSelf_returnsFullProfile() throws Exception {
        User self = new User("bob", "bob@example.com", "hashed");
        self.setId("bob-id");
        when(userRepository.findById("bob-id")).thenReturn(Optional.of(self));

        mockMvc.perform(get("/users/bob-id")
                        .header("Authorization", "Bearer " + tokenFor("bob-id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }
}
