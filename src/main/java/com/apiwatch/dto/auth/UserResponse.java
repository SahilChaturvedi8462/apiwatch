package com.apiwatch.dto.auth;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Instant createdAt;
}
