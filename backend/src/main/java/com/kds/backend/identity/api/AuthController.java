package com.kds.backend.identity.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.TokenPair;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

import static com.kds.backend.identity.api.AuthDtos.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "kds_refresh_token";
    private final AuthService authService;
    private final boolean secureCookie;
    private final Duration refreshTokenTtl;

    public AuthController(AuthService authService,
                          @Value("${app.auth.refresh-cookie-secure}") boolean secureCookie,
                          @Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.authService = authService; this.secureCookie = secureCookie; this.refreshTokenTtl = refreshTokenTtl;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return respond(authService.register(request.email(), request.password()), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respond(authService.login(request.email(), request.password()), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(REFRESH_COOKIE) String refreshToken, HttpServletResponse response) {
        return respond(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                       HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
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

    private AuthResponse respond(TokenPair pair, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(pair.refreshToken(), refreshTokenTtl.toSeconds()).toString());
        return new AuthResponse(pair.accessToken(), pair.accessTokenExpiresInSeconds(),
                new UserResponse(pair.userId(), pair.email()));
    }

    private ResponseCookie refreshCookie(String value, long maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/api/v1/auth").maxAge(maxAge).build();
    }
}
