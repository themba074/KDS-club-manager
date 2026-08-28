package com.kds.backend.identity.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.TokenPair;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import static com.kds.backend.identity.api.AuthDtos.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "kds_refresh_token";
    private final AuthService authService;
    private final AuthSessionResponder sessions;

    public AuthController(AuthService authService, AuthSessionResponder sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return sessions.respond(authService.register(request.email(), request.password()), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return sessions.respond(authService.login(request.email(), request.password()), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(REFRESH_COOKIE) String refreshToken, HttpServletResponse response) {
        return sessions.respond(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                       HttpServletResponse response) {
        authService.logout(refreshToken);
        sessions.clear(response);
    }

    @PostMapping("/select-club")
    public AuthResponse selectClub(@AuthenticationPrincipal Jwt principal,
                                   @Valid @RequestBody SelectClubRequest request,
                                   @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                   HttpServletResponse response) {
        return sessions.respond(authService.selectClub(UUID.fromString(principal.getSubject()), refreshToken, request.clubId()), response);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return new MessageResponse("If an account exists, password reset instructions will be sent.");
    }

    @PostMapping("/password-reset/confirm")
    public MessageResponse confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return new MessageResponse("Your password has been reset. You can now log in.");
    }

}
