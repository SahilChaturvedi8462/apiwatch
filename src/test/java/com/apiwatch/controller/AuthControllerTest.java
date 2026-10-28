package com.apiwatch.controller;

import com.apiwatch.dto.auth.AuthResponse;
import com.apiwatch.dto.auth.LoginRequest;
import com.apiwatch.dto.auth.RegisterRequest;
import com.apiwatch.dto.auth.UserResponse;
import com.apiwatch.exception.DuplicateResourceException;
import com.apiwatch.exception.GlobalExceptionHandler;
import com.apiwatch.security.JwtAuthenticationEntryPoint;
import com.apiwatch.security.JwtService;
import com.apiwatch.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint authEntryPoint;

    @Test
    void register_shouldReturn201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.builder()
                        .id(1L)
                        .name("Alice")
                        .email("alice@example.com")
                        .createdAt(Instant.now())
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void register_shouldReturn400OnInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void register_shouldReturn409OnDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("An account with email 'alice@example.com' already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with email 'alice@example.com' already exists"));
    }

    @Test
    void login_shouldReturn200OkOnValidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.builder()
                        .id(1L)
                        .name("Alice")
                        .email("alice@example.com")
                        .createdAt(Instant.now())
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void login_shouldReturn401OnBadCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
