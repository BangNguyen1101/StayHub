package com.quanlynhatro.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Size(max = 320) String identifier,
            @NotBlank @Size(min = 8, max = 200) String password) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 200) String password,
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 30) String phoneNumber) {}

    public record UserResponse(
            Long userId,
            String username,
            String email,
            String fullName,
            String phoneNumber,
            String role,
            boolean active) {}

    public record AuthResponse(String token, String tokenType, UserResponse user) {}

    public record ErrorResponse(String code, String message) {}
}
