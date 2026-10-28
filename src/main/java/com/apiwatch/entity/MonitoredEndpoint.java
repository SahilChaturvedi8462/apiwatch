package com.apiwatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing an external HTTP endpoint registered for health monitoring.
 */
@Entity
@Table(name = "monitored_endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoredEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 20)
    @Builder.Default
    private HealthStatus currentStatus = HealthStatus.UNKNOWN;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
