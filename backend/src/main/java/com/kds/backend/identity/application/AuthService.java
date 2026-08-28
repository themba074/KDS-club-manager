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
    private final ClubService clubs;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository resetTokens, PasswordEncoder passwordEncoder,
                       SecretTokenService secrets, JwtTokenService jwtTokens, Clock clock,
                       @Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl,
                       @Value("${app.auth.password-reset-token-ttl}") Duration resetTokenTtl,
                       PasswordResetDelivery resetDelivery, ClubService clubs) {
        this.users = users; this.refreshTokens = refreshTokens; this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder; this.secrets = secrets; this.jwtTokens = jwtTokens;
        this.clock = clock; this.refreshTokenTtl = refreshTokenTtl; this.resetTokenTtl = resetTokenTtl;
        this.resetDelivery = resetDelivery;
        this.clubs = clubs;
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
        return rotate(rawToken, null, null);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public TokenPair selectClub(UUID userId, String rawToken, UUID clubId) {
        return rotate(rawToken, userId, clubId);
    }

    private TokenPair rotate(String rawToken, UUID expectedUserId, UUID selectedClubId) {
        Instant now = clock.instant();
        if (rawToken == null || rawToken.isBlank()) throw new AuthenticationException("Please log in again.");
        String tokenHash = secrets.hash(rawToken);
        UUID ownerId = refreshTokens.ownerOf(tokenHash)
                .orElseThrow(() -> new AuthenticationException("Please log in again."));
        if (expectedUserId != null && !ownerId.equals(expectedUserId)) {
            throw new AuthenticationException("The session does not belong to this user.");
        }
        // Serialize rotation and club switching for this identity, including token reuse revocation.
        users.lockById(ownerId).orElseThrow(() -> new AuthenticationException("Please log in again."));
        RefreshTokenEntity current = refreshTokens.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationException("The session has expired. Please log in again."));
        if (current.isRotated()) {
            refreshTokens.findAllByFamilyId(current.getFamilyId()).forEach(token -> token.revoke(now));
            throw new AuthenticationException("This session was reused and has been revoked.");
        }
        if (!current.isUsableAt(now)) throw new AuthenticationException("The session has expired. Please log in again.");

        UUID clubId = selectedClubId != null ? selectedClubId : current.getActiveClubId();
        ClubSummary activeClub = clubId == null ? null : clubs.requireMembership(ownerId, clubId);

        String replacement = secrets.generate();
        String replacementHash = secrets.hash(replacement);
        current.rotate(replacementHash, now);
        RefreshTokenEntity next = new RefreshTokenEntity(UUID.randomUUID(), current.getUser(), replacementHash,
                current.getFamilyId(), now.plus(refreshTokenTtl), now);
        next.selectClub(clubId);
        refreshTokens.save(next);
        return pair(current.getUser(), replacement, activeClub);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = secrets.hash(rawToken);
        refreshTokens.ownerOf(tokenHash).ifPresent(ownerId -> {
            users.lockById(ownerId).orElseThrow(() -> new AuthenticationException("Please log in again."));
            // A refresh may already have rotated the supplied token. Revoke its descendants too.
            refreshTokens.findByTokenHash(tokenHash).ifPresent(token ->
                    refreshTokens.findAllByFamilyId(token.getFamilyId()).forEach(session -> session.revoke(clock.instant())));
        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(normalize(email)).ifPresent(user -> {
            users.lockById(user.getId()).orElseThrow(() -> new AuthenticationException("Please try again."));
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
        String tokenHash = secrets.hash(rawToken);
        UUID ownerId = resetTokens.ownerOf(tokenHash)
                .orElseThrow(() -> new AuthenticationException("The password reset link is invalid or expired."));
        // Load the token only after locking, so a concurrent confirmation sees usedAt.
        // Refresh and reset issuance use this same lock and cannot escape reset revocation.
        users.lockById(ownerId).orElseThrow(() -> new AuthenticationException("The password reset link is invalid or expired."));
        Instant now = clock.instant();
        PasswordResetTokenEntity token = resetTokens.findByTokenHash(tokenHash)
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
        return pair(user, rawRefreshToken, null);
    }

    private TokenPair pair(UserEntity user, String refreshToken, ClubSummary activeClub) {
        return new TokenPair(jwtTokens.issue(user, activeClub == null ? null : activeClub.id(),
                activeClub == null ? java.util.List.of() : activeClub.permissions()), refreshToken, jwtTokens.expiresInSeconds(),
                user.getId(), user.getEmail(), activeClub);
    }

    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
