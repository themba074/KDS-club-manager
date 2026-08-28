package com.kds.backend.members.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.MembershipOnboardingService;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository members;
    private final ClubService clubs;
    private final MembershipOnboardingService onboarding;
    private final AuthService authentication;
    private final SecretTokenService secrets;
    private final MemberInvitationDelivery delivery;
    private final Clock clock;
    private final Duration invitationTtl;

    public MemberService(MemberRepository members, ClubService clubs, MembershipOnboardingService onboarding,
                         AuthService authentication, SecretTokenService secrets, MemberInvitationDelivery delivery,
                         Clock clock, @Value("${app.members.invitation-token-ttl}") Duration invitationTtl) {
        this.members = members;
        this.clubs = clubs;
        this.onboarding = onboarding;
        this.authentication = authentication;
        this.secrets = secrets;
        this.delivery = delivery;
        this.clock = clock;
        this.invitationTtl = invitationTtl;
    }

    public List<MemberDirectoryEntry> directory(UUID actor, String search, MemberDirectoryEntry.MemberStatus status) {
        require(actor, Permission.MEMBERS_READ);
        Stream<MemberDirectoryEntry> entries = Stream.concat(members.activeMembers().stream(), members.pendingInvitations().stream());
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
        if (members.membershipEmailExists(normalizedEmail)) {
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

    public InvitationPreview preview(String rawToken) {
        MemberInvitationEntity invitation = usable(rawToken, false);
        return new InvitationPreview(members.clubName(invitation.getClubId()), invitation.getEmail(),
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

    private void require(UUID actor, Permission permission) {
        var club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(permission.name())) {
            throw new AccessDeniedException("You do not have permission for this action.");
        }
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
