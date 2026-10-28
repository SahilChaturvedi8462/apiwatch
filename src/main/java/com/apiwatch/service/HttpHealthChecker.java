package com.apiwatch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * HTTP probe client that polls endpoints with strict connection and read timeouts.
 * Discards response bodies to prevent memory exhaustion (OOM attacks).
 */
@Component
@Slf4j
public class HttpHealthChecker {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String USER_AGENT = "APIWatch-HealthChecker/1.0";

    private final HttpClient httpClient;

    public HttpHealthChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public record ProbeResult(
            Integer statusCode,
            Long responseTimeMs,
            boolean success,
            String errorMessage
    ) {}

    /**
     * Executes an HTTP GET health probe against the target URL.
     */
    public ProbeResult probe(String url) {
        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(READ_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long responseTimeMs = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();

            // 2xx and 3xx are treated as successful/UP
            boolean success = (statusCode >= 200 && statusCode < 400);
            String errorMessage = success ? null : "HTTP response code: " + statusCode;

            return new ProbeResult(statusCode, responseTimeMs, success, errorMessage);

        } catch (HttpTimeoutException e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            log.debug("Health check timed out for URL '{}': {}", url, e.getMessage());
            return new ProbeResult(null, responseTimeMs, false, "Connection or read timed out after 5 seconds");

        } catch (IOException e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            log.debug("I/O error during health check for URL '{}': {}", url, e.getMessage());
            return new ProbeResult(null, responseTimeMs, false, truncateError("Connection failed: " + e.getMessage()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long responseTimeMs = System.currentTimeMillis() - startTime;
            return new ProbeResult(null, responseTimeMs, false, "Health check interrupted");

        } catch (Exception e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            log.warn("Unexpected error checking URL '{}': {}", url, e.getMessage());
            return new ProbeResult(null, responseTimeMs, false, truncateError("Unexpected error: " + e.getMessage()));
        }
    }

    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
