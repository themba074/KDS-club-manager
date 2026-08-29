package com.kds.backend.members.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.clubtypeconfig.application.RoleDefinition;
import com.kds.backend.clubtypeconfig.application.RoleService;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipOnboardingService;
import com.kds.backend.identity.application.MemberIdentityDirectoryService;
import com.kds.backend.identity.application.MembershipLifecycleMember;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.IdentityDirectoryMember;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberServiceTests {
    private final MemberRepository repository = mock(MemberRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final MembershipOnboardingService onboarding = mock(MembershipOnboardingService.class);
    private final MemberIdentityDirectoryService identityDirectory = mock(MemberIdentityDirectoryService.class);
    private final MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
    private final RoleService roles = mock(RoleService.class);
    private final AuthService authentication = mock(AuthService.class);
    private final SecretTokenService secrets = mock(SecretTokenService.class);
    private final MemberInvitationDelivery delivery = mock(MemberInvitationDelivery.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T08:00:00Z"), ZoneOffset.UTC);
    private final UUID actor = UUID.randomUUID();
    private final UUID clubId = UUID.randomUUID();
    private final MemberService service = new MemberService(repository, clubs, onboarding, identityDirectory, lifecycle, roles, authentication,
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
        verifyNoInteractions(repository, identityDirectory);
    }

    @Test void directoryFiltersCombinedActiveAndInvitedEntries() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(club("MEMBERS_READ"));
        when(identityDirectory.activeMembers()).thenReturn(List.of(new IdentityDirectoryMember(UUID.randomUUID(), "active@example.test", "MEMBER", "ACTIVE", clock.instant())));
        when(repository.profiles()).thenReturn(List.of());
        when(repository.pendingInvitations()).thenReturn(List.of(entry("pending@example.test", "Sipho", MemberDirectoryEntry.MemberStatus.INVITED)));
        var result = service.directory(actor, "sipho", MemberDirectoryEntry.MemberStatus.INVITED);
        assertEquals(List.of("pending@example.test"), result.stream().map(MemberDirectoryEntry::email).toList());
    }

    @Test void statusLifecycleAllowsOnlyDeclaredTransitions() {
        UUID membershipId = UUID.randomUUID();
        when(clubs.requireMembership(actor, clubId)).thenReturn(club("MEMBERS_WRITE"));
        when(lifecycle.requireMembership(membershipId))
                .thenReturn(new MembershipLifecycleMember(membershipId, UUID.randomUUID(), "MEMBER", "ACTIVE"));
        when(roles.requireRole("INVESTMENT_CLUB", "MEMBER"))
                .thenReturn(new RoleDefinition("MEMBER", "Member", Set.of(Permission.MEMBERS_READ)));

        service.changeStatus(actor, membershipId, MemberDirectoryEntry.MemberStatus.SUSPENDED);

        verify(lifecycle).changeStatus(membershipId, "SUSPENDED");
        assertEquals(409, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.changeStatus(actor, membershipId, MemberDirectoryEntry.MemberStatus.ACTIVE))
                .getStatusCode().value());
    }

    @Test void statusLifecycleProtectsTheLastActiveRoleManager() {
        UUID membershipId = UUID.randomUUID();
        var manager = new MembershipLifecycleMember(membershipId, actor, "ADMINISTRATOR", "ACTIVE");
        when(clubs.requireMembership(actor, clubId)).thenReturn(club("MEMBERS_WRITE"));
        when(lifecycle.requireMembership(membershipId)).thenReturn(manager);
        when(lifecycle.memberships()).thenReturn(List.of(manager));
        when(roles.requireRole("INVESTMENT_CLUB", "ADMINISTRATOR"))
                .thenReturn(new RoleDefinition("ADMINISTRATOR", "Administrator", Set.of(Permission.ROLES_MANAGE)));

        var failure = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.changeStatus(actor, membershipId, MemberDirectoryEntry.MemberStatus.EXITED));

        assertEquals(409, failure.getStatusCode().value());
        verify(lifecycle, never()).changeStatus(any(), anyString());
    }

    private ClubSummary club(String... permissions) {
        return new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", false, List.of(permissions));
    }

    private MemberDirectoryEntry entry(String email, String firstName, MemberDirectoryEntry.MemberStatus status) {
        return new MemberDirectoryEntry(UUID.randomUUID(), email, firstName, "Member", null, "MEMBER", status, clock.instant());
    }
}
