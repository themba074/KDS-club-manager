package com.kds.backend.voting.api;

import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MotionIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test void createEditReloadPreservesRetainedVotersAndRejectsStaleWrites() throws Exception {
        Fixture fixture = fixture();
        UUID other = addMember(account(), fixture.clubId(), "MEMBER", "ACTIVE");
        var request = request();
        request.put("options", List.of("Yes", "No", "Abstain"));
        var created = json.readTree(create(fixture.session(), request, 201));
        String id = created.get("id").asString();
        assertEquals(2, created.get("eligibleVoterCount").asInt());
        request.put("title", "Updated budget");
        request.put("options", List.of("Approve", "Reject"));
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(fixture.session()))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.options.length()").value(2)).andExpect(jsonPath("$.eligibleVoterCount").value(2));
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(fixture.session()))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isConflict());
        request.put("version", 1);
        request.put("allActiveMembers", false);
        request.put("eligibleMembershipIds", List.of(other));
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(fixture.session()))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.eligibleVoterCount").value(1));
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(fixture.session())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Updated budget"))
                .andExpect(jsonPath("$[0].options[0].label").value("Approve"))
                .andExpect(jsonPath("$[0].options[1].position").value(1))
                .andExpect(jsonPath("$[0].eligibleMembershipIds[0]").value(other.toString()));
        assertEquals(1, jdbc.queryForObject("select count(*) from motion_eligible_memberships where club_id=? and motion_id=?", Integer.class, fixture.clubId(), UUID.fromString(id)));
        assertEquals(2, jdbc.queryForObject("select count(*) from motion_options where club_id=? and motion_id=?", Integer.class, fixture.clubId(), UUID.fromString(id)));
    }

    @Test void everyEndpointRejectsForeignTenantDataAndUnselectedSessions() throws Exception {
        Fixture first = fixture(), foreign = fixture();
        String id = json.readTree(create(first.session(), request(), 201)).get("id").asString();
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(foreign.session())))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(foreign.session()))
                .contentType("application/json").content(json.writeValueAsString(request()))).andExpect(status().isForbidden());
        cancel(foreign.session(), id, 0, 403);
        var foreignAssignment = request();
        foreignAssignment.put("allActiveMembers", false);
        foreignAssignment.put("eligibleMembershipIds", List.of(membership(first)));
        create(foreign.session(), foreignAssignment, 400);
        TokenPair unselected = account();
        create(unselected, request(), 403);
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(unselected))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/motions")).andExpect(status().isUnauthorized());
        cancel(unselected, id, 0, 403);
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(unselected))
                .contentType("application/json").content(json.writeValueAsString(request()))).andExpect(status().isForbidden());
    }

    @Test void membersCanReadButCannotManageAndIneligibleMembersAreNeverMarkedEligible() throws Exception {
        Fixture fixture = fixture();
        String id = json.readTree(create(fixture.session(), request(), 201)).get("id").asString();
        TokenPair member = account();
        addMember(member, fixture.clubId(), "MEMBER", "ACTIVE");
        TokenPair session = auth.selectClub(member.userId(), member.refreshToken(), fixture.clubId());
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eligibleToVote").value(false))
                .andExpect(jsonPath("$[0].eligibleVoterCount").value(1)).andExpect(jsonPath("$[0].eligibleMembershipIds").isEmpty());
        create(session, request(), 403);
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(session))
                .contentType("application/json").content(json.writeValueAsString(request()))).andExpect(status().isForbidden());
        cancel(session, id, 0, 403);
    }

    @Test void eligibilitySnapshotsExcludeInactiveMembersAndStayStableWhenMembersJoin() throws Exception {
        Fixture fixture = fixture();
        TokenPair eligible = account();
        UUID eligibleId = addMember(eligible, fixture.clubId(), "MEMBER", "ACTIVE");
        UUID suspended = addMember(account(), fixture.clubId(), "MEMBER", "SUSPENDED");
        addMember(account(), fixture.clubId(), "MEMBER", "EXITED");
        create(fixture.session(), request(), 201);
        TokenPair session = auth.selectClub(eligible.userId(), eligible.refreshToken(), fixture.clubId());
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eligibleToVote").value(true))
                .andExpect(jsonPath("$[0].eligibleVoterCount").value(2));
        var invalid = request();
        invalid.put("allActiveMembers", false);
        invalid.put("eligibleMembershipIds", List.of(suspended));
        create(fixture.session(), invalid, 400);
        jdbc.update("update club_memberships set status='SUSPENDED' where club_id=? and id=?", fixture.clubId(), eligibleId);
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(session))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(fixture.session())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eligibleVoterCount").value(2));
    }

    @Test void cancellationSupportsCorrectionsWithoutReopeningAndRejectsRepeatedOrStaleCancellation() throws Exception {
        Fixture fixture = fixture();
        String id = json.readTree(create(fixture.session(), request(), 201)).get("id").asString();
        // Seed elapsed time so this tests server enforcement without a wall-clock sleep.
        jdbc.update("update motions set opens_at=?, closes_at=? where club_id=? and id=?",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusHours(1), fixture.clubId(), UUID.fromString(id));
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(fixture.session())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].state").value("OPEN"));
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(fixture.session()))
                .contentType("application/json").content(json.writeValueAsString(request()))).andExpect(status().isConflict());
        cancel(fixture.session(), id, 7, 409);
        cancel(fixture.session(), id, 0, 200);
        cancel(fixture.session(), id, 1, 409);
        var correction = request();
        correction.put("version", 1);
        correction.put("title", "Corrected cancelled motion");
        correction.put("opensAt", OffsetDateTime.now().minusHours(2));
        correction.put("closesAt", OffsetDateTime.now().minusHours(1));
        mvc.perform(put("/api/v1/motions/" + id).header("Authorization", bearer(fixture.session()))
                .contentType("application/json").content(json.writeValueAsString(correction)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CANCELLED")).andExpect(jsonPath("$.version").value(2));
    }

    @Test void validationRejectsInvalidMotionData() throws Exception {
        Fixture fixture = fixture();
        for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
                "title", " ", "description", "x".repeat(4001), "options", List.of("Yes", " yes "),
                "opensAt", OffsetDateTime.now().minusHours(1), "closesAt", OffsetDateTime.now(), "version", -1).entrySet()) {
            var request = request();
            request.put(invalid.getKey(), invalid.getValue());
            create(fixture.session(), request, 400);
        }
        var request = request();
        request.put("allActiveMembers", false);
        create(fixture.session(), request, 400);
        request.put("allActiveMembers", true);
        request.put("options", Collections.nCopies(21, "Option"));
        create(fixture.session(), request, 400);
        request.put("options", List.of("Yes", "No"));
        request.put("opensAt", null);
        create(fixture.session(), request, 400);
    }

    private record Fixture(TokenPair owner, UUID clubId, TokenPair session) {}
    private Fixture fixture() {
        TokenPair owner = account();
        UUID clubId = clubs.create(owner.userId(), "Voting Club").id();
        return new Fixture(owner, clubId, auth.selectClub(owner.userId(), owner.refreshToken(), clubId));
    }
    private UUID membership(Fixture fixture) {
        return jdbc.queryForObject("select id from club_memberships where club_id=? and user_id=?", UUID.class, fixture.clubId(), fixture.owner().userId());
    }
    private String create(TokenPair session, Map<String, Object> request, int expected) throws Exception {
        return mvc.perform(post("/api/v1/motions").header("Authorization", bearer(session)).contentType("application/json")
                .content(json.writeValueAsString(request))).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }
    private void cancel(TokenPair session, String id, long version, int expected) throws Exception {
        mvc.perform(post("/api/v1/motions/" + id + "/cancel").header("Authorization", bearer(session))
                .contentType("application/json").content(json.writeValueAsString(Map.of("version", version)))).andExpect(status().is(expected));
    }
    private Map<String, Object> request() {
        return new LinkedHashMap<>(Map.of("version", 0, "title", "Approve budget", "description", "Budget vote",
                "opensAt", OffsetDateTime.now().plusHours(1), "closesAt", OffsetDateTime.now().plusDays(1),
                "options", List.of("Yes", "No"), "eligibleMembershipIds", List.of(), "allActiveMembers", true));
    }
    private TokenPair account() { return auth.register(UUID.randomUUID() + "@example.test", "test-password"); }
    private static String bearer(TokenPair session) { return "Bearer " + session.accessToken(); }
    private UUID addMember(TokenPair user, UUID clubId, String role, String status) {
        UUID membershipId = UUID.randomUUID();
        jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?,?,CURRENT_TIMESTAMP)",
                membershipId, clubId, user.userId(), role, status);
        return membershipId;
    }
}
