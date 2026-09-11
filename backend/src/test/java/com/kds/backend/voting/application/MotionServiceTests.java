package com.kds.backend.voting.application;

import com.kds.backend.identity.application.*;
import com.kds.backend.members.application.MemberService;
import com.kds.backend.members.application.VotingEligibleMember;
import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.repository.MotionRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MotionServiceTests {
    private final MotionRepository motions = mock(MotionRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final MembershipLifecycleService memberships = mock(MembershipLifecycleService.class);
    private final MemberService members = mock(MemberService.class);
    private final UUID club = UUID.randomUUID(), actor = UUID.randomUUID(), member = UUID.randomUUID(), id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-05T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private MotionService service;

    @BeforeEach void setup() {
        TenantContext.set(club);
        service = new MotionService(motions, clubs, memberships, members, clock, new MotionMapperImpl());
        when(clubs.requireMembership(actor, club)).thenReturn(new ClubSummary(club, "Club", "INVESTMENT_CLUB", true,
                List.of("VOTES_READ", "VOTES_CREATE", "VOTES_CAST")));
        when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(member, actor, "ADMINISTRATOR", "ACTIVE"));
        when(members.activeVotingMembers()).thenReturn(List.of(new VotingEligibleMember(member, "Member", "member@example.test")));
    }
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void stateTransitionsIncludeExactOpeningAndClosingBoundaries() {
        MotionEntity motion = motion(now.plusSeconds(60), now.plusSeconds(120));
        when(motions.all()).thenReturn(List.of(motion));
        assertEquals(MotionView.State.DRAFT, service.all(actor).getFirst().state());
        service = new MotionService(motions, clubs, memberships, members, Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC), new MotionMapperImpl());
        assertEquals(MotionView.State.OPEN, service.all(actor).getFirst().state());
        service = new MotionService(motions, clubs, memberships, members, Clock.fixed(now.plusSeconds(120), ZoneOffset.UTC), new MotionMapperImpl());
        assertEquals(MotionView.State.CLOSED, service.all(actor).getFirst().state());
        motion.cancel(actor, now);
        assertEquals(MotionView.State.CANCELLED, service.all(actor).getFirst().state());
    }

    @Test void openAndClosedMotionsCannotBeEdited() {
        for (Instant closesAt : List.of(now.plusSeconds(60), now)) {
            when(motions.find(id)).thenReturn(Optional.of(motion(now.minusSeconds(60), closesAt)));
            assertStatus(409, () -> service.edit(actor, id, command(List.of("Yes", "No"), Set.of(member), false)));
        }
        verify(motions, never()).flush();
    }

    @Test void cancelledMotionCanBeEditedWithoutReopening() {
        MotionEntity motion = motion(now.minusSeconds(120), now.minusSeconds(60));
        motion.cancel(actor, now);
        when(motions.find(id)).thenReturn(Optional.of(motion));
        MotionCommand command = new MotionCommand(0, "Corrected", null, now.minusSeconds(120).atOffset(ZoneOffset.UTC),
                now.minusSeconds(60).atOffset(ZoneOffset.UTC), List.of("Yes", "No"), Set.of(member), false);
        assertEquals(MotionView.State.CANCELLED, service.edit(actor, id, command).state());
        assertEquals("Corrected", motion.getTitle());
    }

    @Test void createSnapshotsActiveMembersAndChecksPermissionUnderLock() {
        MotionView view = service.create(actor, command(List.of(" Yes ", "No"), Set.of(), true));
        assertEquals(1, view.eligibleVoterCount());
        assertEquals(Set.of(member), view.eligibleMembershipIds());
        assertEquals("Yes", view.options().getFirst().label());
        var order = inOrder(clubs, memberships, members);
        order.verify(clubs).requireMembership(actor, club);
        order.verify(memberships).lockClub();
        order.verify(clubs).requireMembership(actor, club);
        order.verify(members).activeVotingMembers();
    }

    @Test void revokedPermissionUnderLockPreventsWrites() {
        when(clubs.requireMembership(actor, club)).thenReturn(
                new ClubSummary(club, "Club", "INVESTMENT_CLUB", true, List.of("VOTES_CREATE")),
                new ClubSummary(club, "Club", "INVESTMENT_CLUB", false, List.of("VOTES_READ")));
        assertThrows(AccessDeniedException.class, () -> service.create(actor, command(List.of("Yes", "No"), Set.of(), true)));
        verifyNoInteractions(motions);
    }

    @Test void rejectsDuplicateOptionsAndInvalidWindows() {
        assertStatus(400, () -> service.create(actor, command(List.of("Yes", " yes "), Set.of(member), false)));
        assertStatus(400, () -> service.create(actor, command(List.of("Only option"), Set.of(member), false)));
        assertStatus(400, () -> service.create(actor, command(List.of("Yes", " "), Set.of(member), false)));
        for (Instant opening : List.of(now, now.plusSeconds(120))) {
            MotionCommand invalid = new MotionCommand(0, "Title", null, opening.atOffset(ZoneOffset.UTC),
                    now.plusSeconds(60).atOffset(ZoneOffset.UTC), List.of("Yes", "No"), Set.of(member), false);
            assertStatus(400, () -> service.create(actor, invalid));
        }
        verify(motions, never()).add(any());
    }

    @Test void rejectsEmptyAndForeignOrInactiveVoterSets() {
        assertStatus(400, () -> service.create(actor, command(List.of("Yes", "No"), Set.of(), false)));
        assertStatus(400, () -> service.create(actor, command(List.of("Yes", "No"), Set.of(UUID.randomUUID()), false)));
        when(members.activeVotingMembers()).thenReturn(List.of());
        assertStatus(400, () -> service.create(actor, command(List.of("Yes", "No"), Set.of(), true)));
    }

    @Test void staleEditsAndCancellationsAreRejected() {
        when(motions.find(id)).thenReturn(Optional.of(motion(now.plusSeconds(60), now.plusSeconds(120))));
        MotionCommand stale = new MotionCommand(5, "Changed", null, now.plusSeconds(60).atOffset(ZoneOffset.UTC),
                now.plusSeconds(120).atOffset(ZoneOffset.UTC), List.of("Yes", "No"), Set.of(member), false);
        assertStatus(409, () -> service.edit(actor, id, stale));
        assertStatus(409, () -> service.cancel(actor, id, 5));
        verify(motions, never()).flush();
    }

    @Test void cancellationIsExplicitAndCannotBeRepeated() {
        when(motions.find(id)).thenReturn(Optional.of(motion(now, now.plusSeconds(120))));
        assertEquals(MotionView.State.CANCELLED, service.cancel(actor, id, 0).state());
        assertStatus(409, () -> service.cancel(actor, id, 0));
    }

    @Test void readsHideOtherVoterIdentitiesAndRequireCastPermissionForEligibility() {
        when(motions.all()).thenReturn(List.of(motion(now, now.plusSeconds(120))));
        when(clubs.requireMembership(actor, club)).thenReturn(new ClubSummary(club, "Club", "INVESTMENT_CLUB", false, List.of("VOTES_READ")));
        MotionView view = service.all(actor).getFirst();
        assertTrue(view.eligibleMembershipIds().isEmpty());
        assertFalse(view.eligibleToVote());
        when(clubs.requireMembership(actor, club)).thenReturn(new ClubSummary(club, "Club", "INVESTMENT_CLUB", false, List.of("VOTES_READ", "VOTES_CAST")));
        assertTrue(service.all(actor).getFirst().eligibleToVote());
        when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(UUID.randomUUID(), actor, "MEMBER", "ACTIVE"));
        assertFalse(service.all(actor).getFirst().eligibleToVote());
    }

    @Test void missingTenantAndReadPermissionAreRejected() {
        when(clubs.requireMembership(actor, club)).thenReturn(new ClubSummary(club, "Club", "INVESTMENT_CLUB", false, List.of()));
        assertThrows(AccessDeniedException.class, () -> service.all(actor));
        TenantContext.clear();
        assertThrows(AccessDeniedException.class, () -> service.all(actor));
    }

    private MotionCommand command(List<String> options, Set<UUID> eligible, boolean all) {
        return new MotionCommand(0, " Motion ", null, now.plusSeconds(60).atOffset(ZoneOffset.UTC),
                now.plusSeconds(120).atOffset(ZoneOffset.UTC), options, eligible, all);
    }
    private MotionEntity motion(Instant opensAt, Instant closesAt) {
        MotionEntity motion = new MotionEntity(id, club, actor, now);
        motion.update("Motion", null, opensAt, closesAt, List.of("Yes", "No"), Set.of(member), now);
        return motion;
    }
    private void assertStatus(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
}
