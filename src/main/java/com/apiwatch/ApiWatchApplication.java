package com.apiwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Main application entry point for APIWatch.
 */
@SpringBootApplication
@RestController
public class ApiWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiWatchApplication.class, args);
    }

    /**
     * Public ping endpoint for connectivity verification.
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "UP",
                "service", "APIWatch",
                "message", "pong"
        );
    }
}
