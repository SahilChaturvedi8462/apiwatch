package com.apiwatch.service;

import com.apiwatch.dto.auth.AuthResponse;
import com.apiwatch.dto.auth.LoginRequest;
import com.apiwatch.dto.auth.RegisterRequest;
import com.apiwatch.dto.auth.UserResponse;
import com.apiwatch.entity.User;
import com.apiwatch.exception.DuplicateResourceException;
import com.apiwatch.exception.ResourceNotFoundException;
import com.apiwatch.repository.UserRepository;
import com.apiwatch.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration and JWT authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user, hashes password with BCrypt, and returns a signed JWT.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' already exists");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id={} email={}", savedUser.getId(), savedUser.getEmail());

        String token = jwtService.generateToken(savedUser);
        return mapToAuthResponse(savedUser, token);
    }

    /**
     * Authenticates user credentials via AuthenticationManager and returns a signed JWT.
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));

        log.info("User logged in successfully: id={} email={}", user.getId(), user.getEmail());

        String token = jwtService.generateToken(user);
        return mapToAuthResponse(user, token);
    }

    private AuthResponse mapToAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }
}
