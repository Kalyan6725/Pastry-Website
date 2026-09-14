package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.auth.AuthResponse;
import org.example.backend.dto.auth.LoginRequest;
import org.example.backend.dto.auth.RegisterRequest;
import org.example.backend.dto.auth.UserResponse;
import org.example.backend.security.UserPrincipal;
import org.example.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return new UserResponse(principal.getId(), principal.getUser().getName(), principal.getUser().getEmail(), principal.getUser().getRole(), principal.getUser().isActive());
    }
}
