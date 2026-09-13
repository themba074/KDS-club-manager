package com.kds.backend.voting.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.application.MemberService;
import com.kds.backend.members.application.VotingEligibleMember;
import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.domain.MotionState;
import com.kds.backend.voting.repository.MotionRepository;
import com.kds.backend.voting.repository.VoteRepository;
import com.kds.backend.config.events.DomainEventPublisher;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MotionService {
    private static final Logger LOGGER=LoggerFactory.getLogger(MotionService.class);
    private final MotionRepository motions;
    private final ClubService clubs;
    private final MembershipLifecycleService memberships;
    private final MemberService members;
    private final Clock clock;
    private final MotionMapper mapper;
    private final VoteRepository votes;
    private final DomainEventPublisher events;

    public MotionService(MotionRepository motions, ClubService clubs, MembershipLifecycleService memberships,
                         MemberService members, Clock clock, MotionMapper mapper, VoteRepository votes,DomainEventPublisher events) {
        this.motions = motions;
        this.clubs = clubs;
        this.memberships = memberships;
        this.members = members;
        this.clock = clock;
        this.mapper = mapper;
        this.votes = votes;
        this.events = events;
    }

    public List<MotionView> all(UUID actor) {
        ClubSummary club = require(actor, Permission.VOTES_READ);
        UUID membership = memberships.requireCurrentMembership(actor).membershipId();
        Instant now = clock.instant();
        var selections = votes.selections(membership);
        return motions.all().stream().map(motion -> view(motion, membership, club, now, selections.get(motion.getId()))).toList();
    }

    @Transactional
    public MotionView create(UUID actor, MotionCommand command) {
        require(actor, Permission.VOTES_CREATE);
        // Serialize snapshots with Identity's role and membership status changes.
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        Eligibility eligibility = eligible(command);Set<UUID> eligible=eligibility.membershipIds();
        Instant now = clock.instant();
        validate(command, now, true);
        MotionEntity motion = new MotionEntity(UUID.randomUUID(), TenantContext.requireClubId(), actor, now);
        apply(motion, command, eligible, now);
        motions.add(motion);
        motions.flush();
        notifySafely(motion,MotionNotificationChanged.Action.SCHEDULED,eligibility.recipients());
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now, null);
    }

    @Transactional
    public MotionView edit(UUID actor, UUID id, MotionCommand command) {
        require(actor, Permission.VOTES_CREATE);
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        MotionEntity motion = requireMotionForWrite(id);
        Instant now = clock.instant();
        MotionState state = motion.stateAt(now);
        if (state != MotionState.DRAFT && state != MotionState.CANCELLED) {
            throw conflict("A motion can only be edited before voting opens or after cancellation.");
        }
        requireVersion(motion, command.version());
        if (state == MotionState.CANCELLED && votes.hasVotes(id)) {
            throw conflict("A cancelled motion with recorded votes cannot be edited.");
        }
        Eligibility eligibility = eligible(command);Set<UUID> eligible=eligibility.membershipIds();
        now = clock.instant();
        // Resolving the membership snapshot must not carry an edit across the opening boundary.
        if (state == MotionState.DRAFT && motion.stateAt(now) != MotionState.DRAFT) {
            throw conflict("Voting has opened. Reload this motion.");
        }
        validate(command, now, state != MotionState.CANCELLED);
        // Updating a cancelled motion preserves its cancellation record; it never reopens.
        apply(motion, command, eligible, now);
        motions.flush();
        if(state==MotionState.DRAFT)notifySafely(motion,MotionNotificationChanged.Action.SCHEDULED,eligibility.recipients());
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now, null);
    }

    @Transactional
    public MotionView cancel(UUID actor, UUID id, long version) {
        require(actor, Permission.VOTES_CREATE);
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        MotionEntity motion = requireMotionForWrite(id);
        if (motion.getCancelledAt() != null) throw conflict("This motion is already cancelled.");
        if (motion.getResultsPublishedAt() != null) throw conflict("Published results are immutable; this motion cannot be cancelled.");
        requireVersion(motion, version);
        Instant now = clock.instant();
        motion.cancel(actor, now);
        motions.flush();
        notifySafely(motion,MotionNotificationChanged.Action.CANCELLED,List.of());
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now, null);
    }

    private void apply(MotionEntity motion, MotionCommand command, Set<UUID> eligible, Instant now) {
        motion.update(command.title().strip(), normalize(command.description()), command.opensAt().toInstant(),
                command.closesAt().toInstant(), command.options().stream().map(String::strip).toList(), eligible, now);
    }

    private Eligibility eligible(MotionCommand command) {
        List<VotingEligibleMember> activeMembers=members.activeVotingMembers();
        Set<UUID> active = activeMembers.stream()
                .map(VotingEligibleMember::membershipId).collect(Collectors.toSet());
        Set<UUID> selected = command.allActiveMembers() ? active : command.eligibleMembershipIds();
        if (selected == null || selected.isEmpty()) throw bad("Select at least one eligible active member.");
        if (!active.containsAll(selected)) throw bad("Eligible voters must be active members of this club.");
        Set<UUID> snapshot=Set.copyOf(selected);
        return new Eligibility(snapshot,activeMembers.stream().filter(member->snapshot.contains(member.membershipId())).toList());
    }

    private void notifySafely(MotionEntity motion,MotionNotificationChanged.Action action,List<VotingEligibleMember> recipients) {
        try {
            events.publish(new MotionNotificationChanged(action,motion.getClubId(),motion.getId(),motion.getTitle(),
                    motion.getOpensAt(),motion.getClosesAt(),recipients));
        } catch(RuntimeException failure) {
            LOGGER.warn("Motion saved but notification publication failed motionId={}",motion.getId(),failure);
        }
    }
    private record Eligibility(Set<UUID> membershipIds,List<VotingEligibleMember> recipients){}

    private void validate(MotionCommand command, Instant now, boolean requireFutureOpening) {
        if (command.opensAt() == null || command.closesAt() == null
                || (requireFutureOpening && !command.opensAt().toInstant().isAfter(now))
                || !command.closesAt().toInstant().isAfter(command.opensAt().toInstant())) {
            throw bad("Choose a future opening time and a later closing time; cancelled motions may retain past dates.");
        }
        if (command.options() == null || command.options().size() < 2 || command.options().size() > 20
                || command.options().stream().anyMatch(option -> option == null || option.isBlank())) {
            throw bad("Provide between two and twenty options.");
        }
        long distinctOptions = command.options().stream().map(option -> option.strip().toLowerCase(Locale.ROOT))
                .distinct().count();
        if (distinctOptions != command.options().size()) throw bad("Motion options must be unique.");
    }

    private MotionView view(MotionEntity motion, UUID membership, ClubSummary club, Instant now, UUID selectedOptionId) {
        Set<UUID> eligible = motion.getEligibleMembershipIds();
        boolean manager = club.permissions().contains(Permission.VOTES_CREATE.name());
        boolean eligibleToVote = eligible.contains(membership) && club.permissions().contains(Permission.VOTES_CAST.name());
        return mapper.view(motion, motion.stateAt(now), eligible.size(), eligibleToVote,
                manager ? eligible : Set.of(), selectedOptionId);
    }

    private MotionEntity requireMotionForWrite(UUID id) {
        return motions.lock(id).orElseThrow(() -> new AccessDeniedException("Motion is unavailable in this club."));
    }

    private ClubSummary require(UUID actor, Permission permission) {
        ClubSummary club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(permission.name())) throw new AccessDeniedException("You do not have permission for this action.");
        return club;
    }

    private void requireVersion(MotionEntity motion, long version) {
        if (version != motion.getVersion()) throw conflict("This motion changed since you opened it. Reload and try again.");
    }

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
