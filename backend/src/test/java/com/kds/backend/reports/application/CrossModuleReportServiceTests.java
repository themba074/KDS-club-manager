package com.kds.backend.reports.application;

import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.meetings.application.MeetingService;
import com.kds.backend.meetings.application.MeetingView;
import com.kds.backend.members.application.MemberDirectoryEntry;
import com.kds.backend.members.application.MemberService;
import com.kds.backend.voting.application.MotionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrossModuleReportServiceTests {
    private final UUID actor = UUID.randomUUID();
    private final UUID clubId = UUID.randomUUID();
    private final ClubService clubs = mock(ClubService.class);
    private final MemberService members = mock(MemberService.class);
    private final MeetingService meetings = mock(MeetingService.class);
    private final MotionService motions = mock(MotionService.class);
    private final CrossModuleReportService service = new CrossModuleReportService(clubs, members, meetings, motions,
            Clock.fixed(Instant.parse("2026-09-17T10:00:00Z"), ZoneOffset.UTC));

    @BeforeEach void setup() { TenantContext.set(clubId); }
    @AfterEach void cleanup() { TenantContext.clear(); }

    @Test void memberSnapshotUsesPublicDirectoryAndPreservesNames() {
        authorize("REPORTS_READ");
        when(members.directory(actor, null, null)).thenReturn(List.of(new MemberDirectoryEntry(
                UUID.randomUUID(), "sam@example.test", "Sam", null, null, "MANAGER",
                MemberDirectoryEntry.MemberStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"))));
        var snapshot = service.snapshot(actor, "members", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"));
        assertEquals("Sam", snapshot.rows().getFirst().getFirst());
        assertEquals("sam@example.test", snapshot.rows().getFirst().get(1));
        verify(members).directory(actor, null, null);
        verifyNoInteractions(meetings, motions);
    }

    @Test void meetingsAreFilteredByUtcDate() {
        authorize("REPORTS_READ");
        when(meetings.meetings(actor, MeetingService.View.PAST)).thenReturn(List.of(
                new MeetingView(UUID.randomUUID(), 0, "In range", null, OffsetDateTime.parse("2026-09-10T10:00:00Z"), 60, null, null, List.of()),
                new MeetingView(UUID.randomUUID(), 0, "Out of range", null, OffsetDateTime.parse("2026-08-10T10:00:00Z"), 60, null, null, List.of())));
        var snapshot = service.snapshot(actor, "meetings", LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));
        assertEquals(1, snapshot.rows().size());
        assertEquals("In range", snapshot.rows().getFirst().getFirst());
    }

    @Test void reportPermissionIsRequiredBeforeReadingModules() {
        authorize("MEMBERS_READ");
        assertThrows(AccessDeniedException.class, () -> service.snapshot(actor, "members",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")));
        verifyNoInteractions(members, meetings, motions);
    }

    private void authorize(String... permissions) {
        when(clubs.requireMembership(actor, clubId)).thenReturn(new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", true, List.of(permissions)));
    }
}
