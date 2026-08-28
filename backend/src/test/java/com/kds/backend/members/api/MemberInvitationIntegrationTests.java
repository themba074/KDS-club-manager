package com.kds.backend.members.api;

import com.jayway.jsonpath.JsonPath;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.TokenPair;
import com.kds.backend.members.application.MemberInvitationDelivery;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MemberInvitationIntegrationTests.DeliveryConfiguration.class)
class MemberInvitationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService authentication;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;
    @Autowired CapturingDelivery delivery;

    @BeforeEach void resetDelivery() { delivery.rawToken = null; }

    @Test void invitationAcceptanceCreatesAccountMembershipProfileAndSelectedSession() throws Exception {
        TokenPair owner = account();
        ClubSummary club = clubs.create(owner.userId(), "Ubuntu Investors");
        TokenPair ownerSession = authentication.selectClub(owner.userId(), owner.refreshToken(), club.id());

        mvc.perform(post("/api/v1/member-invitations").header("Authorization", bearer(ownerSession))
                        .contentType("application/json").content("""
                        {"email":"new.member@example.test","firstName":"New","lastName":"Member","phone":"0712345678"}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("INVITED"));
        assertNotNull(delivery.rawToken);

        mvc.perform(get("/api/v1/members").header("Authorization", bearer(ownerSession)).param("status", "INVITED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("new.member@example.test"));
        mvc.perform(get("/api/v1/member-invitations/accept").param("token", delivery.rawToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.clubName").value("Ubuntu Investors"))
                .andExpect(jsonPath("$.accountExists").value(false));

        MvcResult accepted = mvc.perform(post("/api/v1/member-invitations/accept").contentType("application/json")
                        .content("{\"token\":\"" + delivery.rawToken + "\",\"password\":\"secure-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.activeClub.id").value(club.id().toString()))
                .andExpect(jsonPath("$.user.email").value("new.member@example.test")).andReturn();
        assertNotNull(accepted.getResponse().getCookie("kds_refresh_token"));
        String memberAccess = JsonPath.read(accepted.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/v1/members").header("Authorization", "Bearer " + memberAccess).param("search", "New"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].firstName").value("New"));
        assertEquals(1, jdbc.queryForObject("select count(*) from member_profiles where club_id = ?", Integer.class, club.id()));
        mvc.perform(post("/api/v1/member-invitations/accept").contentType("application/json")
                        .content("{\"token\":\"" + delivery.rawToken + "\",\"password\":\"secure-password\"}"))
                .andExpect(status().isGone());
    }

    @Test void directoryAndInvitationWritesAreTenantAndPermissionScoped() throws Exception {
        TokenPair firstOwner = account(); ClubSummary firstClub = clubs.create(firstOwner.userId(), "First");
        TokenPair firstSession = authentication.selectClub(firstOwner.userId(), firstOwner.refreshToken(), firstClub.id());
        invite(firstSession, "only.first@example.test");

        TokenPair secondOwner = account(); ClubSummary secondClub = clubs.create(secondOwner.userId(), "Second");
        TokenPair secondSession = authentication.selectClub(secondOwner.userId(), secondOwner.refreshToken(), secondClub.id());
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(secondSession)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.email == 'only.first@example.test')]").isEmpty());

        TokenPair ordinaryMember = account();
        jdbc.update("insert into club_memberships(id, club_id, user_id, role_code, created_at) values (?, ?, ?, 'MEMBER', CURRENT_TIMESTAMP)",
                UUID.randomUUID(), firstClub.id(), ordinaryMember.userId());
        TokenPair memberSession = authentication.selectClub(ordinaryMember.userId(), ordinaryMember.refreshToken(), firstClub.id());
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(memberSession))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/member-invitations").header("Authorization", bearer(memberSession))
                        .contentType("application/json").content(invitationJson("denied@example.test")))
                .andExpect(status().isForbidden());
    }

    @Test void existingAccountCanAcceptWithoutReplacingItsPassword() throws Exception {
        TokenPair existing = authentication.register("existing@example.test", "existing-password");
        TokenPair owner = account(); ClubSummary club = clubs.create(owner.userId(), "Existing Account Club");
        TokenPair ownerSession = authentication.selectClub(owner.userId(), owner.refreshToken(), club.id());
        invite(ownerSession, "existing@example.test");
        mvc.perform(get("/api/v1/member-invitations/accept").param("token", delivery.rawToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountExists").value(true));
        mvc.perform(post("/api/v1/member-invitations/accept").contentType("application/json")
                        .content("{\"token\":\"" + delivery.rawToken + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(existing.userId().toString()));
        assertNotNull(authentication.login("existing@example.test", "existing-password"));
    }

    private void invite(TokenPair session, String email) throws Exception {
        mvc.perform(post("/api/v1/member-invitations").header("Authorization", bearer(session))
                .contentType("application/json").content(invitationJson(email))).andExpect(status().isCreated());
    }
    private String invitationJson(String email) {
        return "{\"email\":\"" + email + "\",\"firstName\":\"First\",\"lastName\":\"Member\"}";
    }
    private TokenPair account() { return authentication.register(UUID.randomUUID() + "@example.test", "test-password"); }
    private String bearer(TokenPair session) { return "Bearer " + session.accessToken(); }

    @TestConfiguration
    static class DeliveryConfiguration {
        @Bean @Primary CapturingDelivery capturingDelivery() { return new CapturingDelivery(); }
    }
    static class CapturingDelivery implements MemberInvitationDelivery {
        volatile String rawToken;
        @Override public void deliver(String email, String rawToken) { this.rawToken = rawToken; }
    }
}
