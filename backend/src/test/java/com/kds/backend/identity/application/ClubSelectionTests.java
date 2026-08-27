package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.RefreshTokenEntity;
import com.kds.backend.identity.domain.UserEntity;
import com.kds.backend.identity.repository.PasswordResetTokenRepository;
import com.kds.backend.identity.repository.RefreshTokenRepository;
import com.kds.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClubSelectionTests {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final SecretTokenService secrets = new SecretTokenService();
    private final JwtTokenService jwt = mock(JwtTokenService.class);
    private final AuthService service = new AuthService(users, refreshTokens, mock(PasswordResetTokenRepository.class),
            mock(PasswordEncoder.class), secrets, jwt, Clock.systemUTC(), Duration.ofDays(7), Duration.ofMinutes(30),
            mock(PasswordResetDelivery.class), clubs);
    private final UserEntity user = new UserEntity(UUID.randomUUID(), "owner@example.test", "hash", Instant.now());
    private final String raw = "test-refresh-token";
    private final RefreshTokenEntity token = new RefreshTokenEntity(UUID.randomUUID(), user, secrets.hash(raw),
            UUID.randomUUID(), Instant.now().plusSeconds(3600), Instant.now());

    private void session() {
        when(refreshTokens.ownerOf(secrets.hash(raw))).thenReturn(Optional.of(user.getId()));
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokens.findByTokenHash(secrets.hash(raw))).thenReturn(Optional.of(token));
    }

    @Test void selectionRotatesAndPersistsValidatedClub() {
        session();
        UUID clubId = UUID.randomUUID();
        ClubSummary club = new ClubSummary(clubId, "Savings", "INVESTMENT_CLUB", true);
        when(clubs.requireMembership(user.getId(), clubId)).thenReturn(club);
        TokenPair result = service.selectClub(user.getId(), raw, clubId);
        ArgumentCaptor<RefreshTokenEntity> next = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokens).save(next.capture());
        assertEquals(clubId, next.getValue().getActiveClubId());
        assertTrue(token.isRotated());
        assertEquals(club, result.activeClub());
        verify(jwt).issue(user, clubId, java.util.List.of());
    }

    @Test void deniedMembershipDoesNotRotateSession() {
        session();
        UUID clubId = UUID.randomUUID();
        when(clubs.requireMembership(user.getId(), clubId)).thenThrow(new AccessDeniedException("Forbidden"));
        assertThrows(AccessDeniedException.class, () -> service.selectClub(user.getId(), raw, clubId));
        assertFalse(token.isRotated());
        verify(refreshTokens, never()).save(any());
    }

    @Test void selectionRejectsCookieOwnerMismatchBeforeMembershipLookup() {
        when(refreshTokens.ownerOf(secrets.hash(raw))).thenReturn(Optional.of(user.getId()));
        assertThrows(AuthenticationException.class, () -> service.selectClub(UUID.randomUUID(), raw, UUID.randomUUID()));
        verifyNoInteractions(clubs, users, jwt);
    }
}
