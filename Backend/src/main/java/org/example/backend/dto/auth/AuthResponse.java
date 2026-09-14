package org.example.backend.dto.auth;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
