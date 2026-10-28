package com.apiwatch.dto.health;

import lombok.*;

import java.time.Instant;

/**
 * DTO representing a health check result.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheckResponse {

    private Long id;
    private Long endpointId;
    private Integer statusCode;
    private Long responseTimeMs;
    private boolean success;
    private Instant checkedAt;
    private String errorMessage;
}
