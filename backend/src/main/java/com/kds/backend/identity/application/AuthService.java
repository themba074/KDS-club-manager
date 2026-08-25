package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.PasswordResetTokenEntity;
import com.kds.backend.identity.domain.RefreshTokenEntity;
import com.kds.backend.identity.domain.UserEntity;
import com.kds.backend.identity.repository.PasswordResetTokenRepository;
import com.kds.backend.identity.repository.RefreshTokenRepository;
import com.kds.backend.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "The email or password is incorrect.";
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final SecretTokenService secrets;
    private final JwtTokenService jwtTokens;
    private final Clock clock;
    private final Duration refreshTokenTtl;
    private final Duration resetTokenTtl;
    private final PasswordResetDelivery resetDelivery;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository resetTokens, PasswordEncoder passwordEncoder,
                       SecretTokenService secrets, JwtTokenService jwtTokens, Clock clock,
                       @Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl,
                       @Value("${app.auth.password-reset-token-ttl}") Duration resetTokenTtl,
                       PasswordResetDelivery resetDelivery) {
        this.users = users; this.refreshTokens = refreshTokens; this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder; this.secrets = secrets; this.jwtTokens = jwtTokens;
        this.clock = clock; this.refreshTokenTtl = refreshTokenTtl; this.resetTokenTtl = resetTokenTtl;
        this.resetDelivery = resetDelivery;
    }

    @Transactional
    public TokenPair register(String email, String password) {
        String normalizedEmail = normalize(email);
        if (users.existsByEmail(normalizedEmail)) throw new EmailAlreadyRegisteredException();
        Instant now = clock.instant();
        UserEntity user = users.save(new UserEntity(UUID.randomUUID(), normalizedEmail,
                passwordEncoder.encode(password), now));
        return newSession(user, UUID.randomUUID(), now);
    }

    @Transactional
    public TokenPair login(String email, String password) {
        UserEntity user = users.findByEmail(normalize(email))
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
        return newSession(user, UUID.randomUUID(), clock.instant());
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public TokenPair refresh(String rawToken) {
        Instant now = clock.instant();
        RefreshTokenEntity current = refreshTokens.findByTokenHash(secrets.hash(rawToken))
                .orElseThrow(() -> new AuthenticationException("The session has expired. Please log in again."));
        if (current.isRotated()) {
            refreshTokens.findAllByFamilyId(current.getFamilyId()).forEach(token -> token.revoke(now));
            throw new AuthenticationException("This session was reused and has been revoked.");
        }
        if (!current.isUsableAt(now)) throw new AuthenticationException("The session has expired. Please log in again.");

        String replacement = secrets.generate();
        String replacementHash = secrets.hash(replacement);
        current.rotate(replacementHash, now);
        refreshTokens.save(new RefreshTokenEntity(UUID.randomUUID(), current.getUser(), replacementHash,
                current.getFamilyId(), now.plus(refreshTokenTtl), now));
        return pair(current.getUser(), replacement);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHash(secrets.hash(rawToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(normalize(email)).ifPresent(user -> {
            resetTokens.deleteByUserId(user.getId());
            String rawToken = secrets.generate();
            Instant now = clock.instant();
            resetTokens.save(new PasswordResetTokenEntity(UUID.randomUUID(), user, secrets.hash(rawToken),
                    now.plus(resetTokenTtl), now));
            resetDelivery.deliver(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetTokenEntity token = resetTokens.findByTokenHash(secrets.hash(rawToken))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new AuthenticationException("The password reset link is invalid or expired."));
        token.getUser().changePassword(passwordEncoder.encode(newPassword), now);
        token.markUsed(now);
        refreshTokens.deleteByUserId(token.getUser().getId());
    }

    private TokenPair newSession(UserEntity user, UUID familyId, Instant now) {
        String rawRefreshToken = secrets.generate();
        refreshTokens.save(new RefreshTokenEntity(UUID.randomUUID(), user, secrets.hash(rawRefreshToken),
                familyId, now.plus(refreshTokenTtl), now));
        return pair(user, rawRefreshToken);
    }

    private TokenPair pair(UserEntity user, String refreshToken) {
        return new TokenPair(jwtTokens.issue(user), refreshToken, jwtTokens.expiresInSeconds(),
                user.getId(), user.getEmail());
    }

    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
