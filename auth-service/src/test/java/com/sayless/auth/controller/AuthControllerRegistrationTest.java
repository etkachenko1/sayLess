package com.sayless.auth.controller;

import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;
import com.sayless.auth.security.JwtAuthFilter;
import com.sayless.auth.security.JwtUtil;
import com.sayless.auth.security.JwtVerifier;
import com.sayless.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression coverage for registration password validation: a minimum length that was
// previously 6 (raised to 8), the uppercase-letter-and-digit requirement, and a
// common-password blocklist, none of which existed before.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtVerifier.class})
@TestPropertySource(properties = {
    "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789",
    "cors.allowed-origins=http://localhost:5173"
})
class AuthControllerRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register_emailWithNoAtSign_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"user1\",\"password\":\"CorrectHorseBattery9\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailWithNoTld_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"user1@example\",\"password\":\"CorrectHorseBattery9\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordUnderMinLength_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"short1\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordWithoutUppercase_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"lowercase1\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordWithoutDigit_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"NoDigitsHere\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_commonPassword_isRejected() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_validPassword_isHashedBeforeSaving() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"CorrectHorseBattery9\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedPassword = captor.getValue().getPassword();

        assertNotEquals("CorrectHorseBattery9", storedPassword);
        assertTrue(storedPassword.matches("^\\$2[aby]\\$.*"), "expected a BCrypt hash, got: " + storedPassword);
    }
}
