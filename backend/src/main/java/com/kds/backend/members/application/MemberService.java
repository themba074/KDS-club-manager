package com.kds.backend.members.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.clubtypeconfig.application.RoleService;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.MembershipOnboardingService;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MemberIdentityDirectoryService;
import com.kds.backend.identity.application.SecretTokenService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.identity.application.TokenPair;
import com.kds.backend.members.domain.MemberInvitationEntity;
import com.kds.backend.members.domain.MemberProfileEntity;
import com.kds.backend.members.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository members;
    private final ClubService clubs;
    private final MembershipOnboardingService onboarding;
    private final MemberIdentityDirectoryService identityDirectory;
    private final MembershipLifecycleService lifecycle;
    private final RoleService roles;
    private final AuthService authentication;
    private final SecretTokenService secrets;
    private final MemberInvitationDelivery delivery;
    private final Clock clock;
    private final Duration invitationTtl;

    public MemberService(MemberRepository members, ClubService clubs, MembershipOnboardingService onboarding,
                         MemberIdentityDirectoryService identityDirectory, MembershipLifecycleService lifecycle,
                         RoleService roles,
                         AuthService authentication, SecretTokenService secrets, MemberInvitationDelivery delivery,
                         Clock clock, @Value("${app.members.invitation-token-ttl}") Duration invitationTtl) {
        this.members = members;
        this.clubs = clubs;
        this.onboarding = onboarding;
        this.identityDirectory = identityDirectory;
        this.lifecycle = lifecycle;
        this.roles = roles;
        this.authentication = authentication;
        this.secrets = secrets;
        this.delivery = delivery;
        this.clock = clock;
        this.invitationTtl = invitationTtl;
    }

    public List<MemberDirectoryEntry> directory(UUID actor, String search, MemberDirectoryEntry.MemberStatus status) {
        require(actor, Permission.MEMBERS_READ);
        Map<UUID, MemberProfileEntity> profiles = members.profiles().stream()
                .collect(Collectors.toMap(MemberProfileEntity::getMembershipId, Function.identity()));
        Stream<MemberDirectoryEntry> active = identityDirectory.activeMembers().stream().map(member -> {
            MemberProfileEntity profile = profiles.get(member.membershipId());
            return new MemberDirectoryEntry(member.membershipId(), member.email(),
                    profile == null ? null : profile.getFirstName(), profile == null ? null : profile.getLastName(),
                    profile == null ? null : profile.getPhone(), member.roleCode(),
                    MemberDirectoryEntry.MemberStatus.valueOf(member.status()), member.joinedAt());
        });
        Stream<MemberDirectoryEntry> entries = Stream.concat(active, members.pendingInvitations().stream());
        if (status != null) entries = entries.filter(entry -> entry.status() == status);
        if (search != null && !search.isBlank()) {
            String term = search.strip().toLowerCase(Locale.ROOT);
            entries = entries.filter(entry -> Stream.of(entry.email(), entry.firstName(), entry.lastName(), entry.phone())
                    .filter(value -> value != null).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(term)));
        }
        return entries.sorted(Comparator.comparing(MemberDirectoryEntry::email)).toList();
    }

    @Transactional
    public MemberDirectoryEntry invite(UUID actor, String email, String firstName, String lastName, String phone) {
        require(actor, Permission.MEMBERS_WRITE);
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (identityDirectory.membershipEmailExists(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This person is already a club member.");
        }
        if (members.invitationEmailExists(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invitation has already been sent to this email address.");
        }
        String rawToken = secrets.generate();
        Instant now = clock.instant();
        MemberInvitationEntity invitation = new MemberInvitationEntity(UUID.randomUUID(), TenantContext.requireClubId(),
                normalizedEmail, firstName.strip(), lastName.strip(), normalizePhone(phone), "MEMBER",
                secrets.hash(rawToken), now.plus(invitationTtl), actor, now);
        members.saveInvitation(invitation);
        delivery.deliver(normalizedEmail, rawToken);
        return invitedEntry(invitation);
    }

    @Transactional
    public void changeStatus(UUID actor, UUID membershipId, MemberDirectoryEntry.MemberStatus requestedStatus) {
        lifecycle.lockClub();
        ClubSummary club = require(actor, Permission.MEMBERS_WRITE);
        if (requestedStatus == null || requestedStatus == MemberDirectoryEntry.MemberStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an active, suspended, or exited status.");
        }
        var target = lifecycle.requireMembership(membershipId);
        MemberDirectoryEntry.MemberStatus currentStatus = MemberDirectoryEntry.MemberStatus.valueOf(target.status());
        if (!allowedTransition(currentStatus, requestedStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Membership cannot move from " + currentStatus.name().toLowerCase(Locale.ROOT) + " to "
                            + requestedStatus.name().toLowerCase(Locale.ROOT) + ".");
        }
        boolean removingManager = currentStatus == MemberDirectoryEntry.MemberStatus.ACTIVE
                && requestedStatus != MemberDirectoryEntry.MemberStatus.ACTIVE
                && roles.requireRole(club.clubType(), target.roleCode()).permissions().contains(Permission.ROLES_MANAGE);
        if (removingManager) {
            long activeManagers = lifecycle.memberships().stream()
                    .filter(member -> "ACTIVE".equals(member.status()))
                    .filter(member -> roles.requireRole(club.clubType(), member.roleCode()).permissions().contains(Permission.ROLES_MANAGE))
                    .count();
            if (activeManagers <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Assign another active administrator before suspending or exiting the last role manager.");
            }
        }
        lifecycle.changeStatus(membershipId, requestedStatus.name());
        LOGGER.info("membership_status_changed clubId={} actorId={} membershipId={} from={} to={}",
                TenantContext.requireClubId(), actor, membershipId, currentStatus, requestedStatus);
    }

    public InvitationPreview preview(String rawToken) {
        MemberInvitationEntity invitation = usable(rawToken, false);
        return new InvitationPreview(clubs.invitationClubName(invitation.getClubId()), invitation.getEmail(),
                invitation.getFirstName(), invitation.getLastName(), invitation.getRoleCode(),
                onboarding.accountExists(invitation.getEmail()), invitation.getExpiresAt());
    }

    @Transactional
    public TokenPair accept(String rawToken, String password) {
        MemberInvitationEntity invitation = usable(rawToken, true);
        var result = onboarding.accept(invitation.getEmail(), password, invitation.getClubId(), invitation.getRoleCode());
        Instant now = clock.instant();
        members.saveProfile(new MemberProfileEntity(result.membershipId(), invitation.getClubId(),
                invitation.getFirstName(), invitation.getLastName(), invitation.getPhone(), now));
        invitation.accept(now);
        return authentication.startInvitationSession(result.userId(), invitation.getClubId());
    }

    private MemberInvitationEntity usable(String rawToken, boolean lock) {
        if (rawToken == null || rawToken.isBlank()) throw invalidInvitation();
        String hash = secrets.hash(rawToken);
        MemberInvitationEntity invitation = (lock ? members.lockByTokenHash(hash) : members.byTokenHash(hash))
                .orElseThrow(this::invalidInvitation);
        if (!invitation.isUsableAt(clock.instant())) throw invalidInvitation();
        return invitation;
    }

    private ResponseStatusException invalidInvitation() {
        return new ResponseStatusException(HttpStatus.GONE, "This invitation link is invalid, expired, or already used.");
    }

    private ClubSummary require(UUID actor, Permission permission) {
        var club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(permission.name())) {
            throw new AccessDeniedException("You do not have permission for this action.");
        }
        return club;
    }

    private boolean allowedTransition(MemberDirectoryEntry.MemberStatus current, MemberDirectoryEntry.MemberStatus requested) {
        return switch (current) {
            case ACTIVE -> requested == MemberDirectoryEntry.MemberStatus.SUSPENDED
                    || requested == MemberDirectoryEntry.MemberStatus.EXITED;
            case SUSPENDED -> requested == MemberDirectoryEntry.MemberStatus.ACTIVE
                    || requested == MemberDirectoryEntry.MemberStatus.EXITED;
            case INVITED, EXITED -> false;
        };
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.strip();
    }

    private MemberDirectoryEntry invitedEntry(MemberInvitationEntity invitation) {
        return new MemberDirectoryEntry(invitation.getId(), invitation.getEmail(), invitation.getFirstName(),
                invitation.getLastName(), invitation.getPhone(), invitation.getRoleCode(),
                MemberDirectoryEntry.MemberStatus.INVITED, invitation.getCreatedAt());
    }
}
