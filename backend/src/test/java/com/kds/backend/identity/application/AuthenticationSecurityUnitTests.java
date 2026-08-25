package com.kds.backend.identity.application;

import com.kds.backend.config.AuthenticationSecurityConfiguration;
import com.kds.backend.identity.domain.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationSecurityUnitTests {
    private final AuthenticationSecurityConfiguration configuration = new AuthenticationSecurityConfiguration();

    @Test
    void bcryptHashesAndVerifiesPasswordsWithoutStoringPlaintext() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        String hash = encoder.encode("a-secure-password");
        assertNotEquals("a-secure-password", hash);
        assertTrue(encoder.matches("a-secure-password", hash));
        assertFalse(encoder.matches("wrong-password", hash));
    }

    @Test
    void issuedJwtHasExpectedIdentityIssuerAndExpiry() {
        String secret = "a-test-signing-secret-that-is-long-enough-for-hs256";
        JwtEncoder encoder = configuration.jwtEncoder(secret);
        JwtDecoder decoder = configuration.jwtDecoder(secret);
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UserEntity user = new UserEntity(UUID.randomUUID(), "member@example.com", "hash", now);
        String token = new JwtTokenService(encoder, Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(15)).issue(user);
        Jwt jwt = decoder.decode(token);
        assertEquals(JwtTokenService.ISSUER, jwt.getIssuer().toString());
        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals(now.plusSeconds(900), jwt.getExpiresAt());
    }

    @Test
    void generatedOpaqueSecretsAreRandomAndHashDeterministically() {
        SecretTokenService service = new SecretTokenService();
        String first = service.generate();
        String second = service.generate();
        assertNotEquals(first, second);
        assertEquals(service.hash(first), service.hash(first));
        assertNotEquals(first, service.hash(first));
    }
}
