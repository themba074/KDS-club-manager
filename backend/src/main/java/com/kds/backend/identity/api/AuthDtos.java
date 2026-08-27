package com.kds.backend.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.kds.backend.identity.application.ClubSummary;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password) {}

    public record PasswordResetRequest(@NotBlank @Email @Size(max = 320) String email) {}

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record UserResponse(UUID id, String email) {}

    public record AuthResponse(String accessToken, long expiresInSeconds, UserResponse user, ClubSummary activeClub) {}

    public record SelectClubRequest(@NotNull UUID clubId) {}

    public record MessageResponse(String message) {}
}
