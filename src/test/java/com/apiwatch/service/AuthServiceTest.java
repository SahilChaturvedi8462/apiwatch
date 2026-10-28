package com.apiwatch.service;

import com.apiwatch.dto.auth.AuthResponse;
import com.apiwatch.dto.auth.LoginRequest;
import com.apiwatch.dto.auth.RegisterRequest;
import com.apiwatch.entity.User;
import com.apiwatch.exception.DuplicateResourceException;
import com.apiwatch.repository.UserRepository;
import com.apiwatch.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hashedSecret")
                .build();
    }

    @Test
    void register_shouldHashPasswordAndReturnAuthResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("plainSecret")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainSecret")).thenReturn("hashedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresInMs());
        assertEquals("john@example.com", response.getUser().getEmail());

        verify(passwordEncoder).encode("plainSecret");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowDuplicateResourceExceptionWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("plainSecret")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("plainSecret")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(sampleUser, null));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getAccessToken());
        assertEquals("john@example.com", response.getUser().getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowWhenCredentialsInvalid() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongSecret")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
