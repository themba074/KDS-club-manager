package com.kds.backend.identity.api;

import com.kds.backend.identity.application.TokenPair;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.kds.backend.identity.api.AuthDtos.AuthResponse;
import static com.kds.backend.identity.api.AuthDtos.UserResponse;

@Component
public class AuthSessionResponder {
    private static final String REFRESH_COOKIE = "kds_refresh_token";
    private final boolean secureCookie;
    private final Duration refreshTokenTtl;

    public AuthSessionResponder(@Value("${app.auth.refresh-cookie-secure}") boolean secureCookie,
                                @Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.secureCookie = secureCookie;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public AuthResponse respond(TokenPair pair, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(pair.refreshToken(), refreshTokenTtl.toSeconds()).toString());
        return new AuthResponse(pair.accessToken(), pair.accessTokenExpiresInSeconds(),
                new UserResponse(pair.userId(), pair.email()), pair.activeClub());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    private ResponseCookie cookie(String value, long maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/api/v1/auth").maxAge(maxAge).build();
    }
}
