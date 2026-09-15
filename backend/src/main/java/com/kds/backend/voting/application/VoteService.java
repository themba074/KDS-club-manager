package com.kds.backend.voting.application;

import com.kds.backend.audit.application.AuditLogService;
import com.kds.backend.audit.domain.AuditAction;
import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.domain.MotionResultOptionEntity;
import com.kds.backend.voting.domain.MotionState;
import com.kds.backend.voting.domain.VoteEntity;
import com.kds.backend.voting.repository.MotionRepository;
import com.kds.backend.voting.repository.MotionResultRepository;
import com.kds.backend.voting.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VoteService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteService.class);
    private final MotionRepository motions;
    private final VoteRepository votes;
    private final MotionResultRepository results;
    private final ClubService clubs;
    private final MembershipLifecycleService memberships;
    private final AuditLogService audit;
    private final Clock clock;

    public VoteService(MotionRepository motions, VoteRepository votes, MotionResultRepository results,
                       ClubService clubs, MembershipLifecycleService memberships, AuditLogService audit, Clock clock) {
        this.motions = motions;
        this.votes = votes;
        this.results = results;
        this.clubs = clubs;
        this.memberships = memberships;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public VoteReceipt cast(UUID actor, UUID motionId, UUID optionId) {
        require(actor, Permission.VOTES_CAST);
        memberships.lockClub();
        require(actor, Permission.VOTES_CAST);
        UUID membershipId = memberships.requireCurrentMembership(actor).membershipId();
        MotionEntity motion = lockMotion(motionId);
        var now = clock.instant();
        if (motion.stateAt(now) != MotionState.OPEN) throw conflict("Votes can only be cast while the motion is open.");
        if (!motion.getEligibleMembershipIds().contains(membershipId)) {
            throw new AccessDeniedException("You are not eligible to vote on this motion.");
        }
        var option = motion.getOptions().stream().filter(candidate -> candidate.getId().equals(optionId)).findFirst()
                .orElseThrow(() -> new AccessDeniedException("The selected option is unavailable for this motion."));
        if (votes.exists(motionId, membershipId)) throw conflict("You have already voted on this motion.");
        UUID voteId = UUID.randomUUID();
        votes.add(new VoteEntity(voteId, TenantContext.requireClubId(), motionId, optionId, membershipId, now));
        votes.flush();
        // The selected option is deliberately absent from the audit record.
        audit.record(actor, AuditAction.VOTE_CAST, "VOTE", voteId, null, null);
        LOGGER.info("vote_cast clubId={} actorId={} motionId={}", TenantContext.requireClubId(), actor, motionId);
        return new VoteReceipt(motionId, optionId, option.getLabel(), now.atOffset(ZoneOffset.UTC));
    }

    public MotionResultView results(UUID actor, UUID motionId) {
        ClubSummary club = require(actor, Permission.VOTES_READ);
        memberships.requireCurrentMembership(actor);
        MotionEntity motion = findMotion(motionId);
        MotionState state = motion.stateAt(clock.instant());
        if (state == MotionState.CANCELLED) throw conflict("Cancelled motions do not have results.");
        boolean published = motion.getResultsPublishedAt() != null;
        if (!published && state != MotionState.CLOSED) {
            throw conflict("Results are available after voting closes.");
        }
        if (!published && !club.permissions().contains(Permission.VOTES_CREATE.name())) {
            throw new AccessDeniedException("Results have not been published.");
        }
        return published ? publishedResult(motion) : liveResult(motion);
    }

    @Transactional
    public MotionResultView publish(UUID actor, UUID motionId, long version) {
        require(actor, Permission.VOTES_CREATE);
        memberships.lockClub();
        require(actor, Permission.VOTES_CREATE);
        MotionEntity motion = lockMotion(motionId);
        if (motion.getVersion() != version) throw conflict("This motion changed since you opened it. Reload and try again.");
        if (motion.getResultsPublishedAt() != null) throw conflict("Results are already published and cannot be changed.");
        if (motion.stateAt(clock.instant()) != MotionState.CLOSED) throw conflict("Results can only be published after voting closes.");

        Tally tally = tally(motion);
        var now = clock.instant();
        List<MotionResultOptionEntity> snapshots = tally.options().stream()
                .map(option -> new MotionResultOptionEntity(UUID.randomUUID(), TenantContext.requireClubId(), motionId,
                        option.optionId(), option.position(), option.label(), option.voteCount()))
                .toList();
        results.addAll(snapshots);
        motion.publishResults(actor, now, tally.outcome().name(), tally.winningOptionId(), tally.totalVotes());
        motions.flush();
        audit.record(actor, AuditAction.VOTE_RESULTS_PUBLISHED, "MOTION", motionId, null, tally.outcome().name());
        LOGGER.info("motion_results_published clubId={} actorId={} motionId={} outcome={} totalVotes={}",
                TenantContext.requireClubId(), actor, motionId, tally.outcome(), tally.totalVotes());
        return new MotionResultView(motionId, true, now.atOffset(ZoneOffset.UTC), tally.totalVotes(),
                motion.getEligibleMembershipIds().size(), tally.outcome(), tally.winningOptionId(), tally.options());
    }

    private MotionResultView liveResult(MotionEntity motion) {
        Tally tally = tally(motion);
        return new MotionResultView(motion.getId(), false, null, tally.totalVotes(),
                motion.getEligibleMembershipIds().size(), tally.outcome(), tally.winningOptionId(), tally.options());
    }

    private MotionResultView publishedResult(MotionEntity motion) {
        List<MotionResultView.OptionResult> options = results.options(motion.getId()).stream()
                .map(option -> new MotionResultView.OptionResult(option.getOptionId(), option.getPosition(),
                        option.getLabel(), option.getVoteCount())).toList();
        return new MotionResultView(motion.getId(), true,
                OffsetDateTime.ofInstant(motion.getResultsPublishedAt(), ZoneOffset.UTC), motion.getTotalVotes(),
                motion.getEligibleMembershipIds().size(), MotionResultView.Outcome.valueOf(motion.getResultOutcome()),
                motion.getWinningOptionId(), options);
    }

    private Tally tally(MotionEntity motion) {
        Map<UUID, Integer> counts = votes.counts(motion.getId()).stream()
                .collect(Collectors.toMap(VoteRepository.OptionCount::optionId, count -> Math.toIntExact(count.count())));
        List<MotionResultView.OptionResult> options = new ArrayList<>();
        motion.getOptions().forEach(option -> options.add(new MotionResultView.OptionResult(option.getId(),
                option.getPosition(), option.getLabel(), counts.getOrDefault(option.getId(), 0))));
        int total = options.stream().mapToInt(MotionResultView.OptionResult::voteCount).sum();
        UUID winner = options.stream().filter(option -> option.voteCount() > total / 2).map(MotionResultView.OptionResult::optionId)
                .findFirst().orElse(null);
        MotionResultView.Outcome outcome = winner == null
                ? MotionResultView.Outcome.NO_MAJORITY : MotionResultView.Outcome.WINNER;
        return new Tally(total, outcome, winner, List.copyOf(options));
    }

    private MotionEntity findMotion(UUID motionId) {
        return motions.find(motionId).orElseThrow(() -> new AccessDeniedException("Motion is unavailable in this club."));
    }

    private MotionEntity lockMotion(UUID motionId) {
        return motions.lock(motionId).orElseThrow(() -> new AccessDeniedException("Motion is unavailable in this club."));
    }

    private ClubSummary require(UUID actor, Permission permission) {
        ClubSummary club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(permission.name())) throw new AccessDeniedException("You do not have permission for this action.");
        return club;
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record Tally(int totalVotes, MotionResultView.Outcome outcome, UUID winningOptionId,
                         List<MotionResultView.OptionResult> options) {}
}
