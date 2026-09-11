package com.kds.backend.voting.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.application.MemberService;
import com.kds.backend.members.application.VotingEligibleMember;
import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.repository.MotionRepository;
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
    private final MotionRepository motions;
    private final ClubService clubs;
    private final MembershipLifecycleService memberships;
    private final MemberService members;
    private final Clock clock;
    private final MotionMapper mapper;

    public MotionService(MotionRepository motions, ClubService clubs, MembershipLifecycleService memberships,
                         MemberService members, Clock clock, MotionMapper mapper) {
        this.motions = motions;
        this.clubs = clubs;
        this.memberships = memberships;
        this.members = members;
        this.clock = clock;
        this.mapper = mapper;
    }

    public List<MotionView> all(UUID actor) {
        ClubSummary club = require(actor, Permission.VOTES_READ);
        UUID membership = memberships.requireCurrentMembership(actor).membershipId();
        Instant now = clock.instant();
        return motions.all().stream().map(motion -> view(motion, membership, club, now)).toList();
    }

    @Transactional
    public MotionView create(UUID actor, MotionCommand command) {
        require(actor, Permission.VOTES_CREATE);
        // Serialize snapshots with Identity's role and membership status changes.
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        Set<UUID> eligible = eligible(command);
        Instant now = clock.instant();
        validate(command, now, true);
        MotionEntity motion = new MotionEntity(UUID.randomUUID(), TenantContext.requireClubId(), actor, now);
        apply(motion, command, eligible, now);
        motions.add(motion);
        motions.flush();
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now);
    }

    @Transactional
    public MotionView edit(UUID actor, UUID id, MotionCommand command) {
        require(actor, Permission.VOTES_CREATE);
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        MotionEntity motion = requireMotion(id);
        Instant now = clock.instant();
        MotionView.State state = state(motion, now);
        if (state != MotionView.State.DRAFT && state != MotionView.State.CANCELLED) {
            throw conflict("A motion can only be edited before voting opens or after cancellation.");
        }
        requireVersion(motion, command.version());
        Set<UUID> eligible = eligible(command);
        now = clock.instant();
        // Resolving the membership snapshot must not carry an edit across the opening boundary.
        if (state == MotionView.State.DRAFT && state(motion, now) != MotionView.State.DRAFT) {
            throw conflict("Voting has opened. Reload this motion.");
        }
        validate(command, now, state != MotionView.State.CANCELLED);
        // Updating a cancelled motion preserves its cancellation record; it never reopens.
        apply(motion, command, eligible, now);
        motions.flush();
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now);
    }

    @Transactional
    public MotionView cancel(UUID actor, UUID id, long version) {
        require(actor, Permission.VOTES_CREATE);
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.VOTES_CREATE);
        MotionEntity motion = requireMotion(id);
        if (motion.getCancelledAt() != null) throw conflict("This motion is already cancelled.");
        requireVersion(motion, version);
        Instant now = clock.instant();
        motion.cancel(actor, now);
        motions.flush();
        return view(motion, memberships.requireCurrentMembership(actor).membershipId(), club, now);
    }

    private void apply(MotionEntity motion, MotionCommand command, Set<UUID> eligible, Instant now) {
        motion.update(command.title().strip(), normalize(command.description()), command.opensAt().toInstant(),
                command.closesAt().toInstant(), command.options().stream().map(String::strip).toList(), eligible, now);
    }

    private Set<UUID> eligible(MotionCommand command) {
        Set<UUID> active = members.activeVotingMembers().stream()
                .map(VotingEligibleMember::membershipId).collect(Collectors.toSet());
        Set<UUID> selected = command.allActiveMembers() ? active : command.eligibleMembershipIds();
        if (selected == null || selected.isEmpty()) throw bad("Select at least one eligible active member.");
        if (!active.containsAll(selected)) throw bad("Eligible voters must be active members of this club.");
        return Set.copyOf(selected);
    }

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

    private MotionView view(MotionEntity motion, UUID membership, ClubSummary club, Instant now) {
        Set<UUID> eligible = motion.getEligibleMembershipIds();
        boolean manager = club.permissions().contains(Permission.VOTES_CREATE.name());
        boolean eligibleToVote = eligible.contains(membership) && club.permissions().contains(Permission.VOTES_CAST.name());
        return mapper.view(motion, state(motion, now), eligible.size(), eligibleToVote, manager ? eligible : Set.of());
    }

    private MotionView.State state(MotionEntity motion, Instant now) {
        if (motion.getCancelledAt() != null) return MotionView.State.CANCELLED;
        if (now.isBefore(motion.getOpensAt())) return MotionView.State.DRAFT;
        return now.isBefore(motion.getClosesAt()) ? MotionView.State.OPEN : MotionView.State.CLOSED;
    }

    private MotionEntity requireMotion(UUID id) {
        return motions.find(id).orElseThrow(() -> new AccessDeniedException("Motion is unavailable in this club."));
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
