package com.movieselect.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record GuestRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 320) String email) {}

    public record AdminLoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String password) {}

    public record AuthResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UUID userId,
            String name,
            String email,
            String role) {}
}
