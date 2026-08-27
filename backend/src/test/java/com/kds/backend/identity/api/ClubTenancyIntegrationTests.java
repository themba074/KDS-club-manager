package com.kds.backend.identity.api;

import com.jayway.jsonpath.JsonPath;
import com.kds.backend.identity.application.*;
import com.kds.backend.identity.repository.CurrentClubRepository;
import com.kds.backend.identity.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubTenancyIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired CurrentClubRepository current;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test void creationAssignsAdministratorAndListsOnlyOwnClubs() throws Exception {
        TokenPair owner = account();
        TokenPair stranger = account();
        mvc.perform(get("/api/v1/clubs").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/v1/clubs").header("Authorization", bearer(owner)).contentType("application/json")
                        .content("{\"name\":\"  First club  \",\"clubType\":\"INVESTMENT_CLUB\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("First club"))
                .andExpect(jsonPath("$.administrator").value(true));
        mvc.perform(get("/api/v1/clubs").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/clubs").header("Authorization", bearer(stranger)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void selectionSurvivesRefreshAndSwitchesBetweenMemberships() throws Exception {
        TokenPair owner = account();
        ClubSummary first = clubs.create(owner.userId(), "First");
        ClubSummary second = clubs.create(owner.userId(), "Second");
        mvc.perform(get("/api/v1/club").header("Authorization", bearer(owner))).andExpect(status().isForbidden());
        MvcResult selected = select(owner.accessToken(), cookie(owner.refreshToken()), first.id());
        String selectedAccess = access(selected);
        mvc.perform(get("/api/v1/club").header("Authorization", "Bearer " + selectedAccess))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.id().toString()));
        assertThrows(AccessDeniedException.class, TenantContext::requireClubId);
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(responseCookie(selected)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeClub.id").value(first.id().toString())).andReturn();
        MvcResult switched = select(access(refreshed), responseCookie(refreshed), second.id());
        mvc.perform(get("/api/v1/club").header("Authorization", "Bearer " + access(switched)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(second.id().toString()));
        mvc.perform(get("/api/v1/club").header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden()); // Previous request must not leak ThreadLocal state.
    }

    @Test void anotherUsersClubCannotBeSelectedOrReadEvenWithSignedClaim() throws Exception {
        TokenPair owner = account();
        TokenPair stranger = account();
        ClubSummary club = clubs.create(owner.userId(), "Private");
        mvc.perform(post("/api/v1/auth/select-club").header("Authorization", bearer(stranger))
                        .cookie(cookie(stranger.refreshToken())).contentType("application/json")
                        .content("{\"clubId\":\"" + club.id() + "\"}"))
                .andExpect(status().isForbidden());
        String unauthorizedClaim = tokens.issue(users.findById(stranger.userId()).orElseThrow(), club.id());
        mvc.perform(get("/api/v1/club").header("Authorization", "Bearer " + unauthorizedClaim))
                .andExpect(status().isForbidden());
        assertThrows(AccessDeniedException.class, TenantContext::requireClubId);
    }

    @Test void selectionRejectsAnotherUsersRefreshCookie() throws Exception {
        TokenPair owner = account();
        TokenPair stranger = account();
        ClubSummary club = clubs.create(owner.userId(), "Private");
        mvc.perform(post("/api/v1/auth/select-club").header("Authorization", bearer(owner))
                        .cookie(cookie(stranger.refreshToken())).contentType("application/json")
                        .content("{\"clubId\":\"" + club.id() + "\"}"))
                .andExpect(status().isUnauthorized());
        assertNotNull(auth.refresh(stranger.refreshToken()));
    }

    @Test void repositoryRejectsCrossTenantIdentifiersAndMissingContext() {
        TokenPair owner = account();
        ClubSummary first = clubs.create(owner.userId(), "First");
        ClubSummary second = clubs.create(owner.userId(), "Second");
        assertThrows(AccessDeniedException.class, () -> current.findById(first.id()));
        TenantContext.set(first.id());
        assertTrue(current.findById(first.id()).isPresent());
        assertTrue(current.findById(second.id()).isEmpty());
    }

    @Test void removedMembershipInvalidatesTenantRequestsAndRefresh() throws Exception {
        TokenPair owner = account();
        ClubSummary club = clubs.create(owner.userId(), "Private");
        TokenPair selected = auth.selectClub(owner.userId(), owner.refreshToken(), club.id());
        jdbc.update("delete from club_memberships where club_id = ? and user_id = ?", club.id(), owner.userId());
        mvc.perform(get("/api/v1/club").header("Authorization", bearer(selected))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie(selected.refreshToken()))).andExpect(status().isForbidden());
    }

    @Test void globalClubEndpointsStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/clubs")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/clubs").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/select-club").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void invalidClubInputIsRejected() throws Exception {
        TokenPair owner = account();
        mvc.perform(post("/api/v1/clubs").header("Authorization", bearer(owner)).contentType("application/json")
                        .content("{\"name\":\" \",\"clubType\":\"SPORTS\"}"))
                .andExpect(status().isBadRequest());
    }

    private TokenPair account() { return auth.register(UUID.randomUUID() + "@example.test", "a-long-test-password"); }
    private String bearer(TokenPair pair) { return "Bearer " + pair.accessToken(); }
    private Cookie cookie(String token) { return new Cookie("kds_refresh_token", token); }
    private Cookie responseCookie(MvcResult result) { return result.getResponse().getCookie("kds_refresh_token"); }
    private String access(MvcResult result) throws Exception { return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"); }
    private MvcResult select(String token, Cookie cookie, UUID clubId) throws Exception {
        return mvc.perform(post("/api/v1/auth/select-club").header("Authorization", "Bearer " + token)
                        .cookie(cookie).contentType("application/json").content("{\"clubId\":\"" + clubId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeClub.id").value(clubId.toString())).andReturn();
    }
}
