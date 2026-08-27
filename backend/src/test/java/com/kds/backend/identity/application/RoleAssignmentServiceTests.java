package com.kds.backend.identity.application;
import com.kds.backend.clubtypeconfig.application.*;
import com.kds.backend.identity.domain.*;
import com.kds.backend.identity.repository.RoleMembershipRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RoleAssignmentServiceTests {
    private final RoleMembershipRepository repository = mock(RoleMembershipRepository.class);
    private final ClubService clubs = mock(ClubService.class);
    private final RoleService roles = mock(RoleService.class);
    private final RoleAssignmentService service = new RoleAssignmentService(repository, clubs, roles);
    private final UUID actor = UUID.randomUUID(), clubId = UUID.randomUUID(), memberId = UUID.randomUUID();
    @BeforeEach void context() { TenantContext.set(clubId); }
    @AfterEach void clear() { TenantContext.clear(); }
    private void manager() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", true, List.of("ROLES_READ", "ROLES_MANAGE")));
    }
    @Test void catalogAndMemberListRequirePermissions() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", false));
        assertThrows(AccessDeniedException.class, () -> service.roles(actor));
        assertThrows(AccessDeniedException.class, () -> service.members(actor));
        verifyNoInteractions(repository, roles);
        manager();
        when(roles.roles("INVESTMENT_CLUB")).thenReturn(List.of());
        when(repository.members()).thenReturn(List.of());
        assertTrue(service.roles(actor).isEmpty());
        assertTrue(service.members(actor).isEmpty());
    }
    @Test void assignmentRechecksActorAfterClubLock() {
        when(clubs.requireMembership(actor, clubId)).thenReturn(new ClubSummary(clubId, "Club", "INVESTMENT_CLUB", false));
        assertThrows(AccessDeniedException.class, () -> service.assign(actor, memberId, "MEMBER"));
        var order = inOrder(repository, clubs);
        order.verify(repository).lockClub();
        order.verify(clubs).requireMembership(actor, clubId);
        verify(repository, never()).membership(any());
    }
    @Test void assignmentUsesValidatedRoleAndScopedMembership() {
        manager();
        var member = new ClubMembershipEntity(memberId, new ClubEntity(clubId, "Club", Instant.now()), actor, false, Instant.now());
        when(repository.membership(memberId)).thenReturn(Optional.of(member));
        when(roles.requireRole("INVESTMENT_CLUB", "MEMBER")).thenReturn(new RoleDefinition("MEMBER", "Member", Set.of()));
        when(roles.requireRole("INVESTMENT_CLUB", "TREASURER")).thenReturn(new RoleDefinition("TREASURER", "Treasurer", Set.of(Permission.CONTRIBUTIONS_WRITE)));
        service.assign(actor, memberId, "TREASURER");
        assertEquals("TREASURER", member.getRoleCode());
    }
    @Test void lastManagerCannotBeRemoved() {
        manager();
        var member = new ClubMembershipEntity(memberId, new ClubEntity(clubId, "Club", Instant.now()), actor, true, Instant.now());
        when(repository.membership(memberId)).thenReturn(Optional.of(member));
        when(repository.members()).thenReturn(List.of(new RoleMember(memberId, "owner@example.test", "ADMINISTRATOR")));
        when(roles.requireRole("INVESTMENT_CLUB", "ADMINISTRATOR")).thenReturn(new RoleDefinition("ADMINISTRATOR", "Administrator", Set.of(Permission.ROLES_MANAGE)));
        when(roles.requireRole("INVESTMENT_CLUB", "MEMBER")).thenReturn(new RoleDefinition("MEMBER", "Member", Set.of()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.assign(actor, memberId, "MEMBER"));
        assertEquals("ADMINISTRATOR", member.getRoleCode());
    }
}

