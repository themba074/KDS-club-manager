package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.ClubMembershipEntity;
import com.kds.backend.identity.repository.ClubAccessRepository;
import com.kds.backend.identity.repository.CurrentClubRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClubServiceTests {
    private final ClubAccessRepository access = mock(ClubAccessRepository.class);
    private final CurrentClubRepository current = mock(CurrentClubRepository.class);
    private final com.kds.backend.clubtypeconfig.application.RoleService roles = mock(com.kds.backend.clubtypeconfig.application.RoleService.class);
    private final ClubService service = new ClubService(access, current, Clock.systemUTC(), Mappers.getMapper(ClubMapper.class), roles);

    @Test void createsAdministratorWithNormalizedName() {
        when(roles.requireRole("INVESTMENT_CLUB", "ADMINISTRATOR")).thenReturn(
            new com.kds.backend.clubtypeconfig.application.RoleDefinition("ADMINISTRATOR", "Administrator", java.util.Set.of()));
        ClubSummary result = service.create(UUID.randomUUID(), "  Savings club  ");
        ArgumentCaptor<ClubMembershipEntity> membership = ArgumentCaptor.forClass(ClubMembershipEntity.class);
        verify(access).create(any(), membership.capture());
        assertTrue(membership.getValue().isAdministrator());
        assertEquals("Savings club", result.name());
        assertEquals("INVESTMENT_CLUB", result.clubType());
    }

    @Test void lookupIsBoundToRequestingUser() {
        UUID userId = UUID.randomUUID();
        when(access.membershipsForUser(userId)).thenReturn(List.of());
        assertTrue(service.listForUser(userId).isEmpty());
        verify(access).membershipsForUser(userId);
    }

    @Test void resolvesPermissionsThroughClubTypeConfiguration() {
        UUID userId = UUID.randomUUID(), clubId = UUID.randomUUID();
        var membership = new ClubMembershipEntity(UUID.randomUUID(),
            new com.kds.backend.identity.domain.ClubEntity(clubId, "Club", java.time.Instant.now()), userId, false, java.time.Instant.now());
        membership.assignRole("TREASURER");
        when(access.membership(userId, clubId)).thenReturn(Optional.of(membership));
        when(roles.requireRole("INVESTMENT_CLUB", "TREASURER")).thenReturn(
            new com.kds.backend.clubtypeconfig.application.RoleDefinition("TREASURER", "Treasurer",
                java.util.Set.of(com.kds.backend.clubtypeconfig.application.Permission.CONTRIBUTIONS_WRITE)));
        assertEquals(List.of("CONTRIBUTIONS_WRITE"), service.requireMembership(userId, clubId).permissions());
    }

    @Test void membershipIsRequired() {
        UUID userId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(access.membership(userId, clubId)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.requireMembership(userId, clubId));
    }

    @Test void currentClubRequiresContextAndScopedRepository() {
        UUID userId = UUID.randomUUID();
        TenantContext.clear();
        assertThrows(AccessDeniedException.class, () -> service.current(userId));
        UUID clubId = UUID.randomUUID();
        TenantContext.set(clubId);
        try {
            when(current.findById(clubId)).thenReturn(Optional.empty());
            assertThrows(AccessDeniedException.class, () -> service.current(userId));
            verify(current).findById(clubId);
            verifyNoInteractions(access);
        } finally {
            TenantContext.clear();
        }
    }
}
