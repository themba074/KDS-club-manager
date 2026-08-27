package com.kds.backend.identity.api;
import com.kds.backend.clubtypeconfig.application.RoleDefinition;
import com.kds.backend.identity.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/v1")
public class RoleController {
    private final RoleAssignmentService roles;
    private final ClubService clubs;
    public RoleController(RoleAssignmentService roles, ClubService clubs) { this.roles = roles; this.clubs = clubs; }
    public record AssignmentRequest(@NotBlank @Size(max = 40) String roleCode) {}
    @GetMapping("/roles") @PreAuthorize("hasAuthority('ROLES_READ')")
    public List<RoleDefinition> roles(@AuthenticationPrincipal Jwt jwt) { return roles.roles(UUID.fromString(jwt.getSubject())); }
    @GetMapping("/role-members") @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public List<RoleMember> members(@AuthenticationPrincipal Jwt jwt) { return roles.members(UUID.fromString(jwt.getSubject())); }
    @PutMapping("/role-members/{membershipId}") @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID membershipId, @Valid @RequestBody AssignmentRequest request) {
        roles.assign(UUID.fromString(jwt.getSubject()), membershipId, request.roleCode());
    }
    @GetMapping("/permissions") @PreAuthorize("isAuthenticated()")
    public List<String> permissions(@AuthenticationPrincipal Jwt jwt) { return clubs.current(UUID.fromString(jwt.getSubject())).permissions(); }
}

