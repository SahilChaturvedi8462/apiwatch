package com.apiwatch.service;

import com.apiwatch.dto.endpoint.EndpointCreateRequest;
import com.apiwatch.dto.endpoint.EndpointResponse;
import com.apiwatch.dto.endpoint.EndpointUpdateRequest;
import com.apiwatch.entity.HealthStatus;
import com.apiwatch.entity.MonitoredEndpoint;
import com.apiwatch.entity.User;
import com.apiwatch.exception.ResourceNotFoundException;
import com.apiwatch.repository.MonitoredEndpointRepository;
import com.apiwatch.validator.SsrfUrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing MonitoredEndpoint CRUD operations with SSRF validation
 * and user ownership enforcement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final MonitoredEndpointRepository endpointRepository;
    private final SsrfUrlValidator ssrfUrlValidator;

    /**
     * Registers a new endpoint for the authenticated user.
     */
    @Transactional
    public EndpointResponse createEndpoint(EndpointCreateRequest request, User user) {
        String safeUrl = ssrfUrlValidator.validateAndNormalize(request.getUrl());

        MonitoredEndpoint endpoint = MonitoredEndpoint.builder()
                .user(user)
                .name(request.getName().trim())
                .url(safeUrl)
                .currentStatus(HealthStatus.UNKNOWN)
                .enabled(true)
                .build();

        MonitoredEndpoint saved = endpointRepository.save(endpoint);
        log.info("User id={} registered new endpoint id={} ('{}')", user.getId(), saved.getId(), saved.getName());

        return mapToResponse(saved);
    }

    /**
     * Retrieves all endpoints belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<EndpointResponse> getEndpointsForUser(User user) {
        return endpointRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a single endpoint by ID, verifying ownership.
     */
    @Transactional(readOnly = true)
    public EndpointResponse getEndpointById(Long id, User user) {
        MonitoredEndpoint endpoint = findAndVerifyOwnership(id, user);
        return mapToResponse(endpoint);
    }

    /**
     * Updates an existing endpoint, re-validating the URL against SSRF and verifying ownership.
     */
    @Transactional
    public EndpointResponse updateEndpoint(Long id, EndpointUpdateRequest request, User user) {
        MonitoredEndpoint endpoint = findAndVerifyOwnership(id, user);
        String safeUrl = ssrfUrlValidator.validateAndNormalize(request.getUrl());

        endpoint.setName(request.getName().trim());
        endpoint.setUrl(safeUrl);
        endpoint.setEnabled(request.getEnabled());

        MonitoredEndpoint updated = endpointRepository.save(endpoint);
        log.info("User id={} updated endpoint id={}", user.getId(), updated.getId());

        return mapToResponse(updated);
    }

    /**
     * Deletes a monitored endpoint, verifying ownership.
     */
    @Transactional
    public void deleteEndpoint(Long id, User user) {
        MonitoredEndpoint endpoint = findAndVerifyOwnership(id, user);
        endpointRepository.delete(endpoint);
        log.info("User id={} deleted endpoint id={}", user.getId(), id);
    }

    /**
     * Helper to load an endpoint and ensure the authenticated user owns it.
     */
    private MonitoredEndpoint findAndVerifyOwnership(Long id, User user) {
        MonitoredEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitored endpoint not found with id: " + id));

        if (!endpoint.getUser().getId().equals(user.getId())) {
            log.warn("Access denied: User id={} attempted to access endpoint id={} owned by user id={}",
                    user.getId(), id, endpoint.getUser().getId());
            throw new AccessDeniedException("You do not have permission to access this endpoint");
        }

        return endpoint;
    }

    private EndpointResponse mapToResponse(MonitoredEndpoint endpoint) {
        return EndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .url(endpoint.getUrl())
                .currentStatus(endpoint.getCurrentStatus())
                .enabled(endpoint.isEnabled())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}
