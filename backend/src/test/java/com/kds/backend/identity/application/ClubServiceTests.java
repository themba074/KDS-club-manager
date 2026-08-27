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
    private final ClubService service = new ClubService(access, current, Clock.systemUTC(), Mappers.getMapper(ClubMapper.class));

    @Test void createsAdministratorWithNormalizedName() {
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
