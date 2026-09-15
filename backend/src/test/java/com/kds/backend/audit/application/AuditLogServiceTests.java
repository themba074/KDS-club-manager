package com.kds.backend.audit.application;

import com.kds.backend.audit.domain.AuditAction;
import com.kds.backend.audit.domain.AuditLogEntity;
import com.kds.backend.audit.repository.AuditLogRepository;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogServiceTests {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);
    private final AuditLogService service = new AuditLogService(repository, clubs, clock);
    private final UUID clubId = UUID.randomUUID(), actorId = UUID.randomUUID(), subjectId = UUID.randomUUID();

    @BeforeEach void setTenant() { TenantContext.set(clubId); }
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void recordUsesCurrentTenantAndClock() {
        service.record(actorId, AuditAction.PAYMENT_RECORDED, "PAYMENT", subjectId, null, "100.00");
        var captor = org.mockito.ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).append(captor.capture());
        var entry = captor.getValue();
        assertEquals(clubId, entry.getClubId());
        assertEquals(actorId, entry.getActorId());
        assertEquals(subjectId, entry.getEntityId());
        assertEquals(clock.instant(), entry.getOccurredAt());
    }

    @Test void nonAdministratorIsDeniedBeforeAnyAuditQuery() {
        when(clubs.requireMembership(actorId, clubId)).thenReturn(new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", false, List.of()));
        assertThrows(AccessDeniedException.class, () -> service.search(actorId, null, null, null, null, 0, 25));
        verifyNoInteractions(repository);
    }

    @Test void administratorFiltersAndPagesWithinTheTenant() {
        administrator();
        var start = Instant.parse("2026-09-01T00:00:00Z");
        var until = Instant.parse("2026-09-16T00:00:00Z");
        when(repository.count(subjectId, AuditAction.ROLE_ASSIGNED, start, until)).thenReturn(26L);
        when(repository.find(subjectId, AuditAction.ROLE_ASSIGNED, start, until, 1, 25)).thenReturn(List.of());
        var page = service.search(actorId, subjectId, AuditAction.ROLE_ASSIGNED,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15), 1, 25);
        assertEquals(26, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.page());
        verify(repository).find(subjectId, AuditAction.ROLE_ASSIGNED, start, until, 1, 25);
    }

    @Test void invalidPaginationAndDateRangeNeverReachRepository() {
        administrator();
        assertThrows(ResponseStatusException.class, () -> service.search(actorId, null, null, null, null, 0, 101));
        assertThrows(ResponseStatusException.class, () -> service.search(actorId, null, null,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 14), 0, 25));
        verifyNoInteractions(repository);
    }

    private void administrator() {
        when(clubs.requireMembership(actorId, clubId)).thenReturn(
                new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", true, List.of("AUDIT_READ")));
    }
}
