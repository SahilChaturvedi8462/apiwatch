package com.apiwatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing an individual health-check probe result.
 */
@Entity
@Table(name = "health_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private MonitoredEndpoint endpoint;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "checked_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant checkedAt = Instant.now();

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }
}
