package com.apiwatch.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API error payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private Instant timestamp = Instant.now();

    private int status;

    private String error;

    private String message;

    private String path;

    private Map<String, String> validationErrors;
}
