package com.sayless.auth.controller;

import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;
import com.sayless.auth.security.JwtAuthFilter;
import com.sayless.auth.security.JwtUtil;
import com.sayless.auth.security.JwtVerifier;
import com.sayless.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression coverage for the login user-enumeration fix: a nonexistent username and a wrong
// password for a real account used to return different error messages, letting an attacker
// build a list of valid usernames without ever guessing a password.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtVerifier.class})
@TestPropertySource(properties = {
    "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789",
    "cors.allowed-origins=http://localhost:5173"
})
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void login_nonexistentUsername_andWrongPasswordForRealAccount_returnIdenticalResponses() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        User real = new User("alice", "alice@example.com", new BCryptPasswordEncoder().encode("RealPassword1"));
        real.setId("alice-id");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(real));

        var nonexistentResult = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"ghost\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        var wrongPasswordResult = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"WrongPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String nonexistentBody = nonexistentResult.getResponse().getContentAsString();
        String wrongPasswordBody = wrongPasswordResult.getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(nonexistentBody, wrongPasswordBody);
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        User real = new User("alice", "alice@example.com", new BCryptPasswordEncoder().encode("RealPassword1"));
        real.setId("alice-id");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(real));
        when(jwtUtil.generateToken("alice-id")).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"RealPassword1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"fake-jwt-token\"}"));
    }
}
