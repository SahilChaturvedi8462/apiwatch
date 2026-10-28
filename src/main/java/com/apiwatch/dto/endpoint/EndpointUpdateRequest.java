package com.apiwatch.dto.endpoint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointUpdateRequest {

    @NotBlank(message = "Endpoint name is required")
    @Size(min = 2, max = 100, message = "Endpoint name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Endpoint URL is required")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String url;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
