package org.example.backend.dto.auth;

import org.example.backend.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active
) {
}
