package com.kds.backend.identity.api;
import com.kds.backend.identity.application.*;
import com.kds.backend.identity.repository.RoleMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(RoleSecurityIntegrationTests.Probe.class)
class RoleSecurityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;
    @Autowired RoleMembershipRepository repository;
    @Autowired RoleAssignmentService assignments;
    // Test-only operations prove permission enforcement without inventing finance/document business endpoints.
    @RestController static class Probe {
        @GetMapping("/api/v1/_test/finance") @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')") public String finance() { return "ok"; }
        @GetMapping("/api/v1/_test/documents") @PreAuthorize("hasAuthority('DOCUMENTS_MANAGE')") public String documents() { return "ok"; }
        @GetMapping("/api/v1/_test/votes") @PreAuthorize("hasAuthority('VOTES_CREATE')") public String votes() { return "ok"; }
        @GetMapping("/api/v1/_test/reading") @PreAuthorize("hasAuthority('CONTRIBUTIONS_READ')") public String reading() { return "ok"; }
    }
    @ParameterizedTest @CsvSource({"CHAIRPERSON,votes,finance", "TREASURER,finance,documents", "SECRETARY,documents,finance", "MEMBER,reading,finance"})
    void roleAllowsAndDeniesOperations(String role, String allowed, String denied) throws Exception {
        var owner = account(); var club = clubs.create(owner.userId(), "Club");
        var member = account(); addMember(member, club.id(), role);
        var session = auth.selectClub(member.userId(), member.refreshToken(), club.id());
        mvc.perform(get("/api/v1/_test/" + allowed).header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/_test/" + denied).header("Authorization", bearer(session))).andExpect(status().isForbidden());
    }
    @Test void administratorCanAssignButCannotCrossTenantOrRemoveLastManager() throws Exception {
        var owner = account(); var club = clubs.create(owner.userId(), "First");
        var foreignOwner = account(); var foreign = clubs.create(foreignOwner.userId(), "Second");
        var member = account(); UUID memberId = addMember(member, club.id(), "MEMBER");
        var session = auth.selectClub(owner.userId(), owner.refreshToken(), club.id());
        mvc.perform(get("/api/v1/roles").header("Authorization", bearer(session))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/role-members").header("Authorization", bearer(session))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/_test/finance").header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/role-members/" + memberId).header("Authorization", bearer(session)).contentType("application/json")
            .content("{\"roleCode\":\"TREASURER\"}")).andExpect(status().isNoContent());
        UUID ownerId = membershipId(owner.userId(), club.id());
        mvc.perform(put("/api/v1/role-members/" + ownerId).header("Authorization", bearer(session)).contentType("application/json")
            .content("{\"roleCode\":\"MEMBER\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/role-members/" + membershipId(foreignOwner.userId(), foreign.id())).header("Authorization", bearer(session))
            .contentType("application/json").content("{\"roleCode\":\"MEMBER\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/role-members/" + memberId).header("Authorization", bearer(session)).contentType("application/json")
            .content("{\"roleCode\":\"UNKNOWN\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/role-members/" + memberId).header("Authorization", bearer(session)).contentType("application/json")
            .content("{\"roleCode\":\"\"}")).andExpect(status().isBadRequest());
        TenantContext.set(club.id());
        try { assertTrue(repository.membership(membershipId(foreignOwner.userId(), foreign.id())).isEmpty()); }
        finally { TenantContext.clear(); }
        assertThrows(org.springframework.security.access.AccessDeniedException.class, repository::members);
    }
    @Test void oldJwtLosesPrivilegesImmediatelyAfterDemotionAndRefreshUpdatesSnapshot() throws Exception {
        var owner = account(); var club = clubs.create(owner.userId(), "Club");
        var other = account(); addMember(other, club.id(), "ADMINISTRATOR");
        var session = auth.selectClub(owner.userId(), owner.refreshToken(), club.id());
        mvc.perform(put("/api/v1/role-members/" + membershipId(owner.userId(), club.id())).header("Authorization", bearer(session))
            .contentType("application/json").content("{\"roleCode\":\"MEMBER\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/role-members").header("Authorization", bearer(session))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/_test/finance").header("Authorization", bearer(session))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/permissions").header("Authorization", bearer(session))).andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
        assertFalse(auth.refresh(session.refreshToken()).activeClub().permissions().contains("ROLES_MANAGE"));
    }
    @Test void roleEndpointsRequireTenantAndAuthentication() throws Exception {
        var user = account();
        for (String path : new String[]{"/roles", "/role-members", "/permissions"}) {
            mvc.perform(get("/api/v1" + path)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1" + path).header("Authorization", bearer(user))).andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/v1/role-members/" + UUID.randomUUID()).header("Authorization", bearer(user))
            .contentType("application/json").content("{\"roleCode\":\"MEMBER\"}")).andExpect(status().isForbidden());
    }

    @Test void concurrentDemotionsCannotRemoveBothManagers() throws Exception {
        var first = account(); var club = clubs.create(first.userId(), "Concurrent");
        var second = account(); UUID secondId = addMember(second, club.id(), "ADMINISTRATOR");
        UUID firstId = membershipId(first.userId(), club.id());
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> demote(first.userId(), firstId, club.id(), start));
            var two = executor.submit(() -> demote(second.userId(), secondId, club.id(), start));
            start.countDown();
            var results = new java.util.ArrayList<>(java.util.List.of(
                one.get(20, java.util.concurrent.TimeUnit.SECONDS), two.get(20, java.util.concurrent.TimeUnit.SECONDS)));
            java.util.Collections.sort(results);
            assertEquals(java.util.List.of(204, 409), results);
        }
    }
    private int demote(UUID actor, UUID membership, UUID club, java.util.concurrent.CountDownLatch start) throws Exception {
        start.await();
        TenantContext.set(club);
        try {
            assignments.assign(actor, membership, "MEMBER");
            return 204;
        } catch (org.springframework.web.server.ResponseStatusException failure) {
            return failure.getStatusCode().value();
        } finally { TenantContext.clear(); }
    }
    private TokenPair account() { return auth.register(UUID.randomUUID() + "@example.test", "test-password"); }
    private String bearer(TokenPair session) { return "Bearer " + session.accessToken(); }
    private UUID membershipId(UUID user, UUID club) {
        return jdbc.queryForObject("select id from club_memberships where user_id = ? and club_id = ?", UUID.class, user, club);
    }
    private UUID addMember(TokenPair user, UUID club, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into club_memberships(id, club_id, user_id, role_code, created_at) values (?, ?, ?, ?, CURRENT_TIMESTAMP)", id, club, user.userId(), role);
        return id;
    }
}
