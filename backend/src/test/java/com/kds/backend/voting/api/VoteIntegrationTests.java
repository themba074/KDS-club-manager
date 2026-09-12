package com.kds.backend.voting.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoteIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    void oneVoteIsRecordedAndReturnedAsTheMembersPrivateSelection() throws Exception {
        Fixture fixture = fixture();
        TokenPair member = account();
        addMember(member, fixture.clubId(), "MEMBER", "ACTIVE");
        TokenPair memberSession = select(member, fixture.clubId());
        JsonNode motion = createMotion(fixture.session());
        open(motionId(motion));
        String optionId = motion.get("options").get(0).get("id").asString();

        mvc.perform(post("/api/v1/motions/{motionId}/votes", motionId(motion))
                        .header("Authorization", bearer(memberSession)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("optionId", optionId))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.optionId").value(optionId))
                .andExpect(jsonPath("$.optionLabel").value("Yes"))
                .andExpect(jsonPath("$.membershipId").doesNotExist());
        mvc.perform(get("/api/v1/motions").header("Authorization", bearer(memberSession)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].selectedOptionId").value(optionId));
        cast(memberSession, motionId(motion), optionId, 409);
        assertEquals(1, jdbc.queryForObject("select count(*) from votes where club_id=? and motion_id=?",
                Integer.class, fixture.clubId(), motionId(motion)));
    }

    @Test
    void concurrentDuplicateSubmissionsProduceOneVoteAndOneConflict() throws Exception {
        Fixture fixture = fixture();
        JsonNode motion = createMotion(fixture.session());
        UUID motionId = motionId(motion);
        open(motionId);
        String optionId = motion.get("options").get(0).get("id").asString();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var responses = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int index = 0; index < 2; index++) {
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mvc.perform(post("/api/v1/motions/{motionId}/votes", motionId)
                                    .header("Authorization", bearer(fixture.session())).contentType("application/json")
                                    .content(json.writeValueAsString(Map.of("optionId", optionId))))
                            .andReturn().getResponse().getStatus();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> statuses = responses.stream().map(response -> {
                try { return response.get(); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).sorted().toList();
            assertEquals(List.of(201, 409), statuses);
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from votes where club_id=? and motion_id=?",
                Integer.class, fixture.clubId(), motionId));
    }

    @Test
    void closedTalliesPublishAsImmutableResultsForMembers() throws Exception {
        Fixture fixture = fixture();
        TokenPair second = account(), third = account();
        addMember(second, fixture.clubId(), "MEMBER", "ACTIVE");
        addMember(third, fixture.clubId(), "MEMBER", "ACTIVE");
        TokenPair secondSession = select(second, fixture.clubId());
        TokenPair thirdSession = select(third, fixture.clubId());
        JsonNode motion = createMotion(fixture.session());
        UUID motionId = motionId(motion);
        String yes = motion.get("options").get(0).get("id").asString();
        String no = motion.get("options").get(1).get("id").asString();
        open(motionId);
        cast(fixture.session(), motionId, yes, 201);
        cast(secondSession, motionId, yes, 201);
        cast(thirdSession, motionId, no, 201);
        close(motionId);

        mvc.perform(get("/api/v1/motions/{motionId}/results", motionId)
                        .header("Authorization", bearer(fixture.session())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.totalVotes").value(3)).andExpect(jsonPath("$.outcome").value("WINNER"))
                .andExpect(jsonPath("$.winningOptionId").value(yes))
                .andExpect(jsonPath("$.options[0].voteCount").value(2))
                .andExpect(jsonPath("$.options[1].voteCount").value(1));
        mvc.perform(get("/api/v1/motions/{motionId}/results", motionId)
                        .header("Authorization", bearer(secondSession))).andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/motions/{motionId}/results/publish", motionId)
                        .header("Authorization", bearer(fixture.session())).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("version", 0))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.outcome").value("WINNER"));
        mvc.perform(get("/api/v1/motions/{motionId}/results", motionId)
                        .header("Authorization", bearer(secondSession)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.options[0].voteCount").value(2));

        cast(secondSession, motionId, no, 409);
        publish(fixture.session(), motionId, 1, 409);
        cancel(fixture.session(), motionId, 1, 409);
        assertEquals(2, jdbc.queryForObject("select count(*) from motion_result_option_counts where club_id=? and motion_id=?",
                Integer.class, fixture.clubId(), motionId));
        assertEquals(2, jdbc.queryForObject("select vote_count from motion_result_option_counts where club_id=? and motion_id=? and option_id=?",
                Integer.class, fixture.clubId(), motionId, UUID.fromString(yes)));
    }

    @Test
    void ineligibleInactiveAndForeignDataAreRejectedAcrossEveryEndpoint() throws Exception {
        Fixture owner = fixture();
        JsonNode motion = createMotion(owner.session());
        UUID motionId = motionId(motion);
        String optionId = motion.get("options").get(0).get("id").asString();
        open(motionId);

        TokenPair lateMember = account();
        addMember(lateMember, owner.clubId(), "MEMBER", "ACTIVE");
        TokenPair lateSession = select(lateMember, owner.clubId());
        cast(lateSession, motionId, optionId, 403);

        TokenPair suspended = account();
        addMember(suspended, owner.clubId(), "MEMBER", "SUSPENDED");
        assertThrowsForbiddenSelection(suspended, owner.clubId());

        Fixture foreign = fixture();
        cast(foreign.session(), motionId, optionId, 403);
        close(motionId);
        mvc.perform(get("/api/v1/motions/{motionId}/results", motionId)
                        .header("Authorization", bearer(foreign.session()))).andExpect(status().isForbidden());
        publish(foreign.session(), motionId, 0, 403);

        JsonNode foreignMotion = createMotion(foreign.session());
        open(motionId);
        String foreignOption = foreignMotion.get("options").get(0).get("id").asString();
        cast(owner.session(), motionId, foreignOption, 403);
    }

    @Test
    void windowPermissionsValidationAndCancelledMotionsAreEnforced() throws Exception {
        Fixture fixture = fixture();
        JsonNode motion = createMotion(fixture.session());
        UUID motionId = motionId(motion);
        String optionId = motion.get("options").get(0).get("id").asString();
        cast(fixture.session(), motionId, optionId, 409);
        result(fixture.session(), motionId, 409);
        publish(fixture.session(), motionId, 0, 409);
        open(motionId);
        mvc.perform(post("/api/v1/motions/{motionId}/votes", motionId)
                        .header("Authorization", bearer(fixture.session())).contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        cast(fixture.session(), motionId, optionId, 201);
        cancel(fixture.session(), motionId, 0, 200);
        mvc.perform(put("/api/v1/motions/{motionId}", motionId)
                        .header("Authorization", bearer(fixture.session())).contentType("application/json")
                        .content(json.writeValueAsString(Map.of(
                                "version", 1,
                                "title", "Changed after ballots",
                                "description", "Must remain immutable",
                                "opensAt", OffsetDateTime.now().minusHours(2),
                                "closesAt", OffsetDateTime.now().minusHours(1),
                                "options", List.of("Changed yes", "Changed no"),
                                "eligibleMembershipIds", List.of(),
                                "allActiveMembers", true))))
                .andExpect(status().isConflict());
        cast(fixture.session(), motionId, optionId, 409);
        result(fixture.session(), motionId, 409);
        publish(fixture.session(), motionId, 1, 409);
    }

    private record Fixture(TokenPair owner, UUID clubId, TokenPair session) {}

    private Fixture fixture() {
        TokenPair owner = account();
        UUID clubId = clubs.create(owner.userId(), "Voting Club").id();
        return new Fixture(owner, clubId, select(owner, clubId));
    }

    private JsonNode createMotion(TokenPair session) throws Exception {
        String content = mvc.perform(post("/api/v1/motions").header("Authorization", bearer(session))
                        .contentType("application/json").content(json.writeValueAsString(motionRequest())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(content);
    }

    private Map<String, Object> motionRequest() {
        return new LinkedHashMap<>(Map.of("version", 0, "title", "Approve budget", "description", "Budget vote",
                "opensAt", OffsetDateTime.now().plusHours(1), "closesAt", OffsetDateTime.now().plusHours(2),
                "options", List.of("Yes", "No"), "eligibleMembershipIds", List.of(), "allActiveMembers", true));
    }

    private void open(UUID motionId) {
        jdbc.update("update motions set opens_at=?, closes_at=? where id=?",
                OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().plusMinutes(5), motionId);
    }

    private void close(UUID motionId) {
        jdbc.update("update motions set opens_at=?, closes_at=? where id=?",
                OffsetDateTime.now().minusMinutes(10), OffsetDateTime.now().minusMinutes(5), motionId);
    }

    private void cast(TokenPair session, UUID motionId, String optionId, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/motions/{motionId}/votes", motionId)
                        .header("Authorization", bearer(session)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("optionId", optionId))))
                .andExpect(status().is(expectedStatus));
    }

    private void result(TokenPair session, UUID motionId, int expectedStatus) throws Exception {
        mvc.perform(get("/api/v1/motions/{motionId}/results", motionId)
                        .header("Authorization", bearer(session))).andExpect(status().is(expectedStatus));
    }

    private void publish(TokenPair session, UUID motionId, long version, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/motions/{motionId}/results/publish", motionId)
                        .header("Authorization", bearer(session)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("version", version))))
                .andExpect(status().is(expectedStatus));
    }

    private void cancel(TokenPair session, UUID motionId, long version, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/motions/{motionId}/cancel", motionId)
                        .header("Authorization", bearer(session)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("version", version))))
                .andExpect(status().is(expectedStatus));
    }

    private void assertThrowsForbiddenSelection(TokenPair user, UUID clubId) {
        try {
            auth.selectClub(user.userId(), user.refreshToken(), clubId);
            throw new AssertionError("Suspended membership selected a club.");
        } catch (org.springframework.security.access.AccessDeniedException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isBlank());
        }
    }

    private TokenPair account() { return auth.register(UUID.randomUUID() + "@example.test", "test-password"); }
    private TokenPair select(TokenPair account, UUID clubId) {
        return auth.selectClub(account.userId(), account.refreshToken(), clubId);
    }
    private UUID addMember(TokenPair user, UUID clubId, String role, String membershipStatus) {
        UUID membershipId = UUID.randomUUID();
        jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?,?,CURRENT_TIMESTAMP)",
                membershipId, clubId, user.userId(), role, membershipStatus);
        return membershipId;
    }
    private static UUID motionId(JsonNode motion) { return UUID.fromString(motion.get("id").asString()); }
    private static String bearer(TokenPair token) { return "Bearer " + token.accessToken(); }
}
