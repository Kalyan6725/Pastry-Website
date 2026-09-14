package org.example.backend.service;

import org.example.backend.dto.auth.AuthResponse;
import org.example.backend.dto.auth.LoginRequest;
import org.example.backend.dto.auth.RegisterRequest;
import org.example.backend.dto.auth.UserResponse;
import org.example.backend.entity.User;
import org.example.backend.enums.Role;
import org.example.backend.exception.DuplicateResourceException;
import org.example.backend.exception.InactiveUserException;
import org.example.backend.exception.UnauthorizedException;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.isActive());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new InactiveUserException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive()));
    }
}
