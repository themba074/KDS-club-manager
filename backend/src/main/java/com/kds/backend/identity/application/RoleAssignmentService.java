package com.kds.backend.identity.application;
import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.clubtypeconfig.application.RoleDefinition;
import com.kds.backend.clubtypeconfig.application.RoleService;
import com.kds.backend.identity.repository.RoleMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;
@Service @Transactional(readOnly = true)
public class RoleAssignmentService {
    private final RoleMembershipRepository memberships;
    private final ClubService clubs;
    private final RoleService roles;
    public RoleAssignmentService(RoleMembershipRepository memberships, ClubService clubs, RoleService roles) {
        this.memberships = memberships; this.clubs = clubs; this.roles = roles;
    }
    public List<RoleDefinition> roles(UUID actor) {
        return roles.roles(require(actor, Permission.ROLES_READ).clubType());
    }
    public List<RoleMember> members(UUID actor) {
        require(actor, Permission.ROLES_MANAGE);
        return memberships.members();
    }
    @Transactional
    public void assign(UUID actor, UUID membershipId, String roleCode) {
        // Serialize changes per club, then re-check the actor: an older JWT cannot bypass a demotion.
        memberships.lockClub();
        ClubSummary club = require(actor, Permission.ROLES_MANAGE);
        var target = memberships.membership(membershipId)
            .orElseThrow(() -> new AccessDeniedException("Membership is unavailable in this club."));
        var replacement = roles.requireRole(club.clubType(), roleCode);
        boolean removingManager = roles.requireRole(club.clubType(), target.getRoleCode()).permissions().contains(Permission.ROLES_MANAGE)
            && !replacement.permissions().contains(Permission.ROLES_MANAGE);
        if (removingManager) {
            long managers = memberships.members().stream()
                .filter(member -> roles.requireRole(club.clubType(), member.roleCode()).permissions().contains(Permission.ROLES_MANAGE)).count();
            if (managers <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Assign another administrator before removing the last role manager.");
        }
        target.assignRole(replacement.code());
    }
    private ClubSummary require(UUID actor, Permission permission) {
        ClubSummary club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(permission.name())) throw new AccessDeniedException("You do not have permission for this action.");
        return club;
    }
}

