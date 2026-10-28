package com.apiwatch.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validator to prevent Server-Side Request Forgery (SSRF) attacks.
 * Rejects non-HTTP/HTTPS schemes, localhost, private IP ranges (RFC 1918),
 * link-local/cloud metadata IPs (169.254.169.254), and loopback addresses.
 */
@Component
@Slf4j
public class SsrfUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "[::1]"
    );

    private static final String CLOUD_METADATA_IP = "169.254.169.254";

    /**
     * Validates that the provided URL is safe to poll.
     * Throws IllegalArgumentException if the URL violates security policies.
     *
     * @param rawUrl The URL string submitted by the user.
     * @return Normalized, validated URL string.
     */
    public String validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        final URI uri;
        try {
            uri = URI.create(rawUrl.trim()).normalize();
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed URL syntax: " + e.getMessage());
        }

        // 1. Validate scheme (HTTP/HTTPS only)
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("Invalid protocol. Only HTTP and HTTPS URLs are permitted");
        }

        // 2. Validate host presence
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must contain a valid host name");
        }

        String lowerHost = host.toLowerCase();

        // 3. Block well-known forbidden hostnames & suffixes
        if (BLOCKED_HOSTNAMES.contains(lowerHost)
                || lowerHost.endsWith(".localhost")
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal")) {
            throw new IllegalArgumentException("Access to local or internal hosts is strictly prohibited");
        }

        // 4. Resolve DNS and inspect resolved IP addresses
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isRestrictedIp(address)) {
                    log.warn("Blocked SSRF attempt targeting host '{}' resolved to IP '{}'", host, address.getHostAddress());
                    throw new IllegalArgumentException("The target URL resolves to a prohibited private, internal, or loopback IP address");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host name: " + host);
        }

        return uri.toString();
    }

    /**
     * Checks if an IP address is private, loopback, link-local, multicast, or cloud metadata.
     */
    private boolean isRestrictedIp(InetAddress address) {
        String ipString = address.getHostAddress();

        if (CLOUD_METADATA_IP.equals(ipString)) {
            return true;
        }

        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        // Additional check for Carrier-Grade NAT (100.64.0.0/10)
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 100.64.0.0 - 100.127.255.255
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                return true;
            }

            // 0.0.0.0/8 (Current network)
            if (b0 == 0) {
                return true;
            }
        }

        return false;
    }
}
