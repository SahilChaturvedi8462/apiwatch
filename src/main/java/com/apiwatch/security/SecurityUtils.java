package com.apiwatch.security;

import com.apiwatch.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility helper to safely extract current authenticated user information.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation
    }

    /**
     * Returns the currently authenticated User entity, or null if not authenticated.
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    /**
     * Returns the currently authenticated user's email, or null.
     */
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }
}
