package com.apiwatch.controller;

import com.apiwatch.dto.common.ErrorResponse;
import com.apiwatch.dto.endpoint.EndpointCreateRequest;
import com.apiwatch.dto.endpoint.EndpointResponse;
import com.apiwatch.dto.endpoint.EndpointUpdateRequest;
import com.apiwatch.dto.health.HealthCheckResponse;
import com.apiwatch.entity.User;
import com.apiwatch.service.EndpointService;
import com.apiwatch.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing monitored endpoints and their health checks.
 * All endpoints require Bearer JWT authentication.
 */
@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Monitored Endpoints", description = "Endpoints for registering, viewing, and managing monitored APIs")
@SecurityRequirement(name = "BearerAuth")
public class EndpointController {

    private final EndpointService endpointService;
    private final HealthCheckService healthCheckService;

    @PostMapping
    @Operation(summary = "Register a new monitored endpoint", description = "Creates a new endpoint to monitor. Validates URL against SSRF.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Endpoint created successfully",
                    content = @Content(schema = @Schema(implementation = EndpointResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or SSRF-prohibited URL",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid JWT)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EndpointResponse> createEndpoint(
            @Valid @RequestBody EndpointCreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        EndpointResponse response = endpointService.createEndpoint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all monitored endpoints", description = "Returns all endpoints belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of endpoints retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EndpointResponse.class))))
    public ResponseEntity<List<EndpointResponse>> getEndpoints(
            @AuthenticationPrincipal User currentUser) {

        List<EndpointResponse> endpoints = endpointService.getEndpointsForUser(currentUser);
        return ResponseEntity.ok(endpoints);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get endpoint by ID", description = "Retrieves details of a specific endpoint owned by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endpoint details retrieved",
                    content = @Content(schema = @Schema(implementation = EndpointResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (Endpoint owned by another user)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endpoint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EndpointResponse> getEndpointById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        EndpointResponse response = endpointService.getEndpointById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an endpoint", description = "Updates endpoint name, URL, or enabled state with SSRF validation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endpoint updated successfully",
                    content = @Content(schema = @Schema(implementation = EndpointResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or SSRF-prohibited URL",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (Endpoint owned by another user)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endpoint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EndpointResponse> updateEndpoint(
            @PathVariable Long id,
            @Valid @RequestBody EndpointUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        EndpointResponse response = endpointService.updateEndpoint(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an endpoint", description = "Deletes an endpoint owned by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Endpoint deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Endpoint owned by another user)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endpoint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        endpointService.deleteEndpoint(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get health check history", description = "Retrieves recent health check probe records for an endpoint")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HealthCheckResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Forbidden (Endpoint owned by another user)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endpoint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<HealthCheckResponse>> getEndpointHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        List<HealthCheckResponse> history = healthCheckService.getHistoryForEndpoint(id, currentUser);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/check")
    @Operation(summary = "Trigger manual health check", description = "Immediately probes the endpoint on-demand and records the result")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health check executed successfully",
                    content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (Endpoint owned by another user)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endpoint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HealthCheckResponse> triggerManualCheck(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        HealthCheckResponse response = healthCheckService.performManualCheck(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
