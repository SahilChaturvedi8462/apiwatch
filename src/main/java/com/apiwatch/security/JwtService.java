package com.apiwatch.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for issuing, signing, parsing, and validating JSON Web Tokens (JWT)
 * using HMAC-SHA256 (JJWT 0.12.x).
 */
@Service
@Slf4j
public class JwtService {

    private final String secret;

    @Getter
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts the subject (username/email) directly from claims.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the expiration date directly from claims.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Parses the signed JWT and extracts all payload claims.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generates a token from UserDetails.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Generates a token from email directly.
     */
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates if the token belongs to the given user and is not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return (username != null
                    && username.equalsIgnoreCase(userDetails.getUsername())
                    && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid or expired JWT token: {}", e.getMessage());
            return false;
        }
    }
}