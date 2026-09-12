package com.kds.backend.voting.application;

import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleMember;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.domain.MotionResultOptionEntity;
import com.kds.backend.voting.repository.MotionRepository;
import com.kds.backend.voting.repository.MotionResultRepository;
import com.kds.backend.voting.repository.VoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoteServiceTests {
    private final MotionRepository motions = mock(MotionRepository.class);
    private final VoteRepository votes = mock(VoteRepository.class);
    private final MotionResultRepository results = mock(MotionResultRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final MembershipLifecycleService memberships = mock(MembershipLifecycleService.class);
    private final UUID clubId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID membershipId = UUID.randomUUID();
    private final UUID motionId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-12T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private VoteService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(clubId);
        service = new VoteService(motions, votes, results, clubs, memberships, clock);
        when(clubs.requireMembership(actorId, clubId)).thenReturn(summary("VOTES_READ", "VOTES_CREATE", "VOTES_CAST"));
        when(memberships.requireCurrentMembership(actorId))
                .thenReturn(new MembershipLifecycleMember(membershipId, actorId, "MEMBER", "ACTIVE"));
    }

    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test
    void eligibleMemberCastsOneImmutableVoteDuringOpenWindow() {
        MotionEntity motion = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(membershipId));
        UUID optionId = motion.getOptions().getFirst().getId();
        when(motions.lock(motionId)).thenReturn(Optional.of(motion));

        VoteReceipt receipt = service.cast(actorId, motionId, optionId);

        assertEquals(optionId, receipt.optionId());
        assertEquals("Yes", receipt.optionLabel());
        verify(votes).add(argThat(vote -> vote.getMotionId().equals(motionId)
                && vote.getMembershipId().equals(membershipId) && vote.getOptionId().equals(optionId)));
        verify(votes).flush();
    }

    @Test
    void duplicateVoteIsRejectedInsteadOfReplaced() {
        MotionEntity motion = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(membershipId));
        when(motions.lock(motionId)).thenReturn(Optional.of(motion));
        when(votes.exists(motionId, membershipId)).thenReturn(true);

        assertStatus(409, () -> service.cast(actorId, motionId, motion.getOptions().getFirst().getId()));
        verify(votes, never()).add(any());
    }

    @Test
    void castingRequiresOpenWindowEligibilityAndOwnedOption() {
        MotionEntity draft = motion(now.plusSeconds(1), now.plusSeconds(30), Set.of(membershipId));
        when(motions.lock(motionId)).thenReturn(Optional.of(draft));
        assertStatus(409, () -> service.cast(actorId, motionId, draft.getOptions().getFirst().getId()));

        MotionEntity ineligible = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(UUID.randomUUID()));
        when(motions.lock(motionId)).thenReturn(Optional.of(ineligible));
        assertThrows(AccessDeniedException.class,
                () -> service.cast(actorId, motionId, ineligible.getOptions().getFirst().getId()));

        MotionEntity open = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(membershipId));
        when(motions.lock(motionId)).thenReturn(Optional.of(open));
        assertThrows(AccessDeniedException.class, () -> service.cast(actorId, motionId, UUID.randomUUID()));
    }

    @Test
    void permissionIsCheckedAgainAfterTheClubLock() {
        when(clubs.requireMembership(actorId, clubId)).thenReturn(summary("VOTES_CAST"), summary("VOTES_READ"));
        assertThrows(AccessDeniedException.class, () -> service.cast(actorId, motionId, UUID.randomUUID()));
        verifyNoInteractions(motions, votes);
    }

    @Test
    void managerCanPreviewClosedMajorityBeforePublication() {
        MotionEntity motion = motion(now.minusSeconds(60), now, Set.of(membershipId, UUID.randomUUID(), UUID.randomUUID()));
        UUID winner = motion.getOptions().getFirst().getId();
        when(motions.find(motionId)).thenReturn(Optional.of(motion));
        when(votes.counts(motionId)).thenReturn(List.of(new VoteRepository.OptionCount(winner, 2),
                new VoteRepository.OptionCount(motion.getOptions().get(1).getId(), 1)));

        MotionResultView result = service.results(actorId, motionId);

        assertFalse(result.published());
        assertEquals(MotionResultView.Outcome.WINNER, result.outcome());
        assertEquals(winner, result.winningOptionId());
        assertEquals(3, result.totalVotes());
    }

    @Test
    void noVotesTieAndPluralityWithoutHalfHaveNoMajority() {
        MotionEntity motion = motion(now.minusSeconds(60), now, Set.of(membershipId));
        when(motions.find(motionId)).thenReturn(Optional.of(motion));
        assertEquals(MotionResultView.Outcome.NO_MAJORITY, service.results(actorId, motionId).outcome());

        when(votes.counts(motionId)).thenReturn(List.of(
                new VoteRepository.OptionCount(motion.getOptions().getFirst().getId(), 1),
                new VoteRepository.OptionCount(motion.getOptions().get(1).getId(), 1)));
        assertEquals(MotionResultView.Outcome.NO_MAJORITY, service.results(actorId, motionId).outcome());
    }

    @Test
    void regularMembersCannotSeeUnpublishedResults() {
        MotionEntity motion = motion(now.minusSeconds(60), now, Set.of(membershipId));
        when(motions.find(motionId)).thenReturn(Optional.of(motion));
        when(clubs.requireMembership(actorId, clubId)).thenReturn(summary("VOTES_READ", "VOTES_CAST"));
        assertThrows(AccessDeniedException.class, () -> service.results(actorId, motionId));
    }

    @Test
    void resultsAreUnavailableBeforeCloseAndForCancelledMotions() {
        MotionEntity open = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(membershipId));
        when(motions.find(motionId)).thenReturn(Optional.of(open));
        assertStatus(409, () -> service.results(actorId, motionId));
        open.cancel(actorId, now);
        assertStatus(409, () -> service.results(actorId, motionId));
    }

    @Test
    void publicationPersistsAnImmutableSnapshotAndCannotRepeat() {
        MotionEntity motion = motion(now.minusSeconds(60), now, Set.of(membershipId));
        UUID winner = motion.getOptions().getFirst().getId();
        when(motions.lock(motionId)).thenReturn(Optional.of(motion));
        when(votes.counts(motionId)).thenReturn(List.of(new VoteRepository.OptionCount(winner, 1)));

        MotionResultView published = service.publish(actorId, motionId, 0);

        assertTrue(published.published());
        assertEquals(MotionResultView.Outcome.WINNER, published.outcome());
        verify(results).addAll(argThat(rows -> rows.size() == 2));
        verify(motions).flush();
        assertStatus(409, () -> service.publish(actorId, motionId, motion.getVersion()));
    }

    @Test
    void publishedReadsUseSnapshotsInsteadOfRecountingVotes() {
        MotionEntity motion = motion(now.minusSeconds(60), now, Set.of(membershipId));
        UUID winner = motion.getOptions().getFirst().getId();
        motion.publishResults(actorId, now, "WINNER", winner, 1);
        when(motions.find(motionId)).thenReturn(Optional.of(motion));
        when(results.options(motionId)).thenReturn(List.of(
                new MotionResultOptionEntity(UUID.randomUUID(), clubId, motionId, winner, 0, "Yes", 1),
                new MotionResultOptionEntity(UUID.randomUUID(), clubId, motionId,
                        motion.getOptions().get(1).getId(), 1, "No", 0)));

        MotionResultView result = service.results(actorId, motionId);

        assertTrue(result.published());
        assertEquals(1, result.totalVotes());
        verify(votes, never()).counts(any());
    }

    @Test
    void staleEarlyCancelledAndRepeatedPublicationAreRejected() {
        MotionEntity closed = motion(now.minusSeconds(60), now, Set.of(membershipId));
        when(motions.lock(motionId)).thenReturn(Optional.of(closed));
        assertStatus(409, () -> service.publish(actorId, motionId, 4));

        MotionEntity open = motion(now.minusSeconds(30), now.plusSeconds(30), Set.of(membershipId));
        when(motions.lock(motionId)).thenReturn(Optional.of(open));
        assertStatus(409, () -> service.publish(actorId, motionId, 0));

        open.cancel(actorId, now);
        assertStatus(409, () -> service.publish(actorId, motionId, 0));
    }

    private MotionEntity motion(Instant opensAt, Instant closesAt, Set<UUID> eligible) {
        MotionEntity motion = new MotionEntity(motionId, clubId, actorId, now.minusSeconds(120));
        motion.update("Budget", null, opensAt, closesAt, List.of("Yes", "No"), eligible, now.minusSeconds(120));
        return motion;
    }

    private ClubSummary summary(String... permissions) {
        return new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", true, List.of(permissions));
    }

    private void assertStatus(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
}
