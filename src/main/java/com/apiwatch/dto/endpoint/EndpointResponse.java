package com.apiwatch.dto.endpoint;

import com.apiwatch.entity.HealthStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointResponse {

    private Long id;
    private String name;
    private String url;
    private HealthStatus currentStatus;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
