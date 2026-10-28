package com.apiwatch.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 60000; // 1 minute

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_shouldCreateValidSignedToken() {
        UserDetails user = new User("test@example.com", "password", Collections.emptyList());

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("test@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalseForMismatchedUser() {
        UserDetails user1 = new User("alice@example.com", "password", Collections.emptyList());
        UserDetails user2 = new User("bob@example.com", "password", Collections.emptyList());

        String token = jwtService.generateToken(user1);

        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        // Create service with 1ms expiration
        JwtService shortLivedService = new JwtService(SECRET, -1000);
        UserDetails user = new User("expired@example.com", "password", Collections.emptyList());

        String token = shortLivedService.generateToken(user);

        assertFalse(shortLivedService.isTokenValid(token, user));
    }
}
