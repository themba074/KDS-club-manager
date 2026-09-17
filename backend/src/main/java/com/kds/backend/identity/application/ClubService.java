package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.ClubEntity;
import com.kds.backend.identity.domain.ClubMembershipEntity;
import com.kds.backend.identity.repository.ClubAccessRepository;
import com.kds.backend.identity.repository.CurrentClubRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClubService {
    private final ClubAccessRepository access;
    private final CurrentClubRepository current;
    private final Clock clock;
    private final ClubMapper mapper;
    private final com.kds.backend.clubtypeconfig.application.RoleService roles;
    private final com.kds.backend.clubtypeconfig.application.ClubTypeConfigService clubTypes;

    public ClubService(ClubAccessRepository access, CurrentClubRepository current, Clock clock, ClubMapper mapper,
                       com.kds.backend.clubtypeconfig.application.RoleService roles,
                       com.kds.backend.clubtypeconfig.application.ClubTypeConfigService clubTypes) {
        this.access = access; this.current = current; this.clock = clock; this.mapper = mapper;
        this.roles = roles;
        this.clubTypes = clubTypes;
    }

    @Transactional
    public ClubSummary create(UUID userId, String name) {
        return create(userId, name, "INVESTMENT_CLUB");
    }

    @Transactional
    public ClubSummary create(UUID userId, String name, String clubType) {
        var template = clubTypes.require(clubType);
        roles.requireRole(template.code(), template.administratorRoleCode());
        roles.requireRole(template.code(), template.defaultMemberRoleCode());
        ClubEntity club = new ClubEntity(UUID.randomUUID(), name.strip(), template.code(), clock.instant());
        ClubMembershipEntity creator = new ClubMembershipEntity(UUID.randomUUID(), club, userId,
                template.administratorRoleCode(), clock.instant());
        access.create(club, creator);
        return summary(creator);
    }

    public List<ClubSummary> listForUser(UUID userId) {
        return access.membershipsForUser(userId).stream().map(this::summary).toList();
    }

    public ClubSummary requireMembership(UUID userId, UUID clubId) {
        return access.membership(userId, clubId).map(this::summary)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this club."));
    }

    public ClubSummary current(UUID userId) {
        UUID clubId = TenantContext.requireClubId();
        current.findById(clubId).orElseThrow(() -> new AccessDeniedException("Club is unavailable."));
        return requireMembership(userId, clubId);
    }

    /** Token-bootstrap lookup used only after the Members module validates an invitation secret. */
    public String invitationClubName(UUID clubId) {
        return access.clubName(clubId).orElseThrow(() -> new AccessDeniedException("The invited club is unavailable."));
    }

    private ClubSummary summary(ClubMembershipEntity membership) {
        ClubSummary club = mapper.summary(membership);
        var permissions = roles.requireRole(club.clubType(), membership.getRoleCode()).permissions()
                .stream().map(Enum::name).sorted().toList();
        boolean administrator = membership.getRoleCode().equals(clubTypes.require(club.clubType()).administratorRoleCode());
        return new ClubSummary(club.id(), club.name(), club.clubType(), administrator, permissions);
    }
}
