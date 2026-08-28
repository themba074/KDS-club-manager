package com.kds.backend.members.application;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipOnboardingService;
import com.kds.backend.identity.application.SecretTokenService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.domain.MemberInvitationEntity;
import com.kds.backend.members.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberServiceTests {
    private final MemberRepository repository = mock(MemberRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final MembershipOnboardingService onboarding = mock(MembershipOnboardingService.class);
    private final AuthService authentication = mock(AuthService.class);
    private final SecretTokenService secrets = mock(SecretTokenService.class);
    private final MemberInvitationDelivery delivery = mock(MemberInvitationDelivery.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T08:00:00Z"), ZoneOffset.UTC);
    private final UUID actor = UUID.randomUUID();
    private final UUID clubId = UUID.randomUUID();
    private final MemberService service = new MemberService(repository, clubs, onboarding, authentication,
            secrets, delivery, clock, Duration.ofDays(7));

    @BeforeEach void setContext() { TenantContext.set(clubId); }
    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test void inviteNormalizesInputAndStoresOnlyTheTokenHash() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(club("MEMBERS_WRITE"));
        when(secrets.generate()).thenReturn("raw-secret");
        when(secrets.hash("raw-secret")).thenReturn("hashed-secret");

        var entry = service.invite(actor, "  PERSON@Example.Test ", " Thandi ", " Ndlovu ", " 012 345 ");

        ArgumentCaptor<MemberInvitationEntity> saved = ArgumentCaptor.forClass(MemberInvitationEntity.class);
        verify(repository).saveInvitation(saved.capture());
        assertEquals("person@example.test", saved.getValue().getEmail());
        assertEquals("hashed-secret", saved.getValue().getTokenHash());
        assertEquals(clock.instant().plus(Duration.ofDays(7)), saved.getValue().getExpiresAt());
        assertEquals(MemberDirectoryEntry.MemberStatus.INVITED, entry.status());
        verify(delivery).deliver("person@example.test", "raw-secret");
    }

    @Test void currentPermissionsAreRecheckedBeforeDirectoryOrInviteAccess() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(club());
        assertThrows(AccessDeniedException.class, () -> service.directory(actor, null, null));
        assertThrows(AccessDeniedException.class, () -> service.invite(actor, "person@example.test", "A", "B", null));
        verifyNoInteractions(repository);
    }

    @Test void directoryFiltersCombinedActiveAndInvitedEntries() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(club("MEMBERS_READ"));
        when(repository.activeMembers()).thenReturn(List.of(entry("active@example.test", "Thandi", MemberDirectoryEntry.MemberStatus.ACTIVE)));
        when(repository.pendingInvitations()).thenReturn(List.of(entry("pending@example.test", "Sipho", MemberDirectoryEntry.MemberStatus.INVITED)));
        var result = service.directory(actor, "sipho", MemberDirectoryEntry.MemberStatus.INVITED);
        assertEquals(List.of("pending@example.test"), result.stream().map(MemberDirectoryEntry::email).toList());
    }

    private ClubSummary club(String... permissions) {
        return new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", false, List.of(permissions));
    }

    private MemberDirectoryEntry entry(String email, String firstName, MemberDirectoryEntry.MemberStatus status) {
        return new MemberDirectoryEntry(UUID.randomUUID(), email, firstName, "Member", null, "MEMBER", status, clock.instant());
    }
}
