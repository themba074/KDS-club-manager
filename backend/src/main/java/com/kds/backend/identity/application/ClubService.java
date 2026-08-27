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

    public ClubService(ClubAccessRepository access, CurrentClubRepository current, Clock clock, ClubMapper mapper,
                       com.kds.backend.clubtypeconfig.application.RoleService roles) {
        this.access = access; this.current = current; this.clock = clock; this.mapper = mapper;
        this.roles = roles;
    }

    @Transactional
    public ClubSummary create(UUID userId, String name) {
        ClubEntity club = new ClubEntity(UUID.randomUUID(), name.strip(), clock.instant());
        ClubMembershipEntity creator = new ClubMembershipEntity(UUID.randomUUID(), club, userId, true, clock.instant());
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

    private ClubSummary summary(ClubMembershipEntity membership) {
        ClubSummary club = mapper.summary(membership);
        var permissions = roles.requireRole(club.clubType(), membership.getRoleCode()).permissions()
                .stream().map(Enum::name).sorted().toList();
        return new ClubSummary(club.id(), club.name(), club.clubType(), club.administrator(), permissions);
    }
}
