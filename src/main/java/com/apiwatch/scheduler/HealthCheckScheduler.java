package com.apiwatch.scheduler;

import com.apiwatch.entity.MonitoredEndpoint;
import com.apiwatch.repository.MonitoredEndpointRepository;
import com.apiwatch.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled polling worker that periodically checks all enabled endpoints.
 * Uses fixedDelay to prevent concurrent or overlapping executions of the check cycle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckScheduler {

    private final MonitoredEndpointRepository endpointRepository;
    private final HealthCheckService healthCheckService;

    /**
     * Periodically iterates through all enabled endpoints and performs health checks.
     * Configured via 'app.scheduler.interval-ms' in application.yml.
     */
    @Scheduled(
            fixedDelayString = "${app.scheduler.interval-ms:60000}",
            initialDelay = 10000
    )
    public void runHealthChecks() {
        List<MonitoredEndpoint> endpoints = endpointRepository.findAllEnabledWithUser();

        if (endpoints.isEmpty()) {
            log.debug("Scheduler: No active endpoints registered for monitoring.");
            return;
        }

        log.info("Scheduler: Beginning health check cycle for {} active endpoint(s)", endpoints.size());

        int succeeded = 0;
        int failed = 0;

        for (MonitoredEndpoint endpoint : endpoints) {
            try {
                var check = healthCheckService.performCheck(endpoint);
                if (check.isSuccess()) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Scheduler: Unexpected failure checking endpoint id={} ('{}'): {}",
                        endpoint.getId(), endpoint.getName(), e.getMessage());
            }
        }

        log.info("Scheduler: Completed health check cycle. Results: {} UP, {} DOWN", succeeded, failed);
    }
}
