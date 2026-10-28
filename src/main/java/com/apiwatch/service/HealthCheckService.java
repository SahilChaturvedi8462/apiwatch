package com.apiwatch.service;

import com.apiwatch.dto.health.HealthCheckResponse;
import com.apiwatch.entity.HealthCheck;
import com.apiwatch.entity.HealthStatus;
import com.apiwatch.entity.MonitoredEndpoint;
import com.apiwatch.entity.User;
import com.apiwatch.exception.ResourceNotFoundException;
import com.apiwatch.repository.HealthCheckRepository;
import com.apiwatch.repository.MonitoredEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service managing health check execution, historical records, and status transitions.
 * Directly calls EmailService when an endpoint transitions between UP and DOWN states.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final HealthCheckRepository healthCheckRepository;
    private final MonitoredEndpointRepository endpointRepository;
    private final HttpHealthChecker httpHealthChecker;
    private final EmailService emailService;

    /**
     * Executes a health check against a monitored endpoint, saves the result,
     * and emails the owner if the health status changed.
     *
     * Note: The endpoint passed here must already have its User loaded eagerly.
     * The scheduler uses findAllEnabledWithUser() (JOIN FETCH) to guarantee this.
     */
    @Transactional
    public HealthCheck performCheck(MonitoredEndpoint endpoint) {
        HttpHealthChecker.ProbeResult probe = httpHealthChecker.probe(endpoint.getUrl());

        HealthCheck healthCheck = HealthCheck.builder()
                .endpoint(endpoint)
                .statusCode(probe.statusCode())
                .responseTimeMs(probe.responseTimeMs())
                .success(probe.success())
                .errorMessage(probe.errorMessage())
                .build();

        HealthCheck saved = healthCheckRepository.save(healthCheck);

        HealthStatus newStatus = probe.success() ? HealthStatus.UP : HealthStatus.DOWN;
        HealthStatus oldStatus = endpoint.getCurrentStatus();

        if (oldStatus != newStatus) {
            endpoint.setCurrentStatus(newStatus);
            endpointRepository.save(endpoint);

            User owner = endpoint.getUser();

            if (oldStatus == HealthStatus.UNKNOWN) {
                // First check — just log, no alert
                log.info("Initial health status for endpoint id={} ('{}'): {}",
                        endpoint.getId(), endpoint.getName(), newStatus);
            } else {
                // Real transition: alert the owner
                log.info("HEALTH TRANSITION endpoint id={} ('{}'): {} -> {} (HTTP {})",
                        endpoint.getId(), endpoint.getName(), oldStatus, newStatus, probe.statusCode());

                if (newStatus == HealthStatus.DOWN) {
                    emailService.sendEndpointDownAlert(
                            owner,
                            endpoint,
                            probe.statusCode(),
                            probe.errorMessage(),
                            Instant.now()
                    );
                } else {
                    emailService.sendEndpointRecoveredAlert(
                            owner,
                            endpoint,
                            probe.statusCode(),
                            Instant.now()
                    );
                }
            }
        }

        return saved;
    }

    /**
     * Returns recent health check history for a specific endpoint,
     * verifying that the requesting user owns it.
     */
    @Transactional(readOnly = true)
    public List<HealthCheckResponse> getHistoryForEndpoint(Long endpointId, User user) {
        verifyEndpointOwnership(endpointId, user);
        return healthCheckRepository.findTop50ByEndpointIdOrderByCheckedAtDesc(endpointId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Triggers an on-demand health check for an endpoint owned by the requesting user.
     */
    @Transactional
    public HealthCheckResponse performManualCheck(Long endpointId, User user) {
        MonitoredEndpoint endpoint = verifyEndpointOwnership(endpointId, user);
        HealthCheck check = performCheck(endpoint);
        return mapToResponse(check);
    }

    private MonitoredEndpoint verifyEndpointOwnership(Long endpointId, User user) {
        MonitoredEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Monitored endpoint not found with id: " + endpointId));

        if (!endpoint.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to view health records for this endpoint");
        }

        return endpoint;
    }

    private HealthCheckResponse mapToResponse(HealthCheck check) {
        return HealthCheckResponse.builder()
                .id(check.getId())
                .endpointId(check.getEndpoint().getId())
                .statusCode(check.getStatusCode())
                .responseTimeMs(check.getResponseTimeMs())
                .success(check.isSuccess())
                .checkedAt(check.getCheckedAt())
                .errorMessage(check.getErrorMessage())
                .build();
    }
}
