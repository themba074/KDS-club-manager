package com.kds.backend.members.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MemberInvitationIntegrationTests.DeliveryConfiguration.class)
class MemberLifecycleImportIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService authentication;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;

    @Test void lifecycleRevokesOldSessionsRejectsInvalidTransitionsAndProtectsTenantBoundaries() throws Exception {
        TokenPair owner = account();
        ClubSummary club = clubs.create(owner.userId(), "Lifecycle Club");
        TokenPair ownerSession = authentication.selectClub(owner.userId(), owner.refreshToken(), club.id());
        TokenPair member = account();
        UUID memberId = addMember(member, club.id(), "MEMBER");
        TokenPair memberSession = authentication.selectClub(member.userId(), member.refreshToken(), club.id());

        changeStatus(ownerSession, memberId, "SUSPENDED", 204);
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(memberSession)))
                .andExpect(status().isForbidden());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> authentication.refresh(memberSession.refreshToken()));
        changeStatus(ownerSession, memberId, "ACTIVE", 204);
        changeStatus(ownerSession, memberId, "EXITED", 204);
        changeStatus(ownerSession, memberId, "ACTIVE", 409);

        TokenPair foreignOwner = account();
        ClubSummary foreignClub = clubs.create(foreignOwner.userId(), "Foreign Club");
        UUID foreignMembership = membershipId(foreignOwner.userId(), foreignClub.id());
        changeStatus(ownerSession, foreignMembership, "SUSPENDED", 403);

        UUID ownerMembership = membershipId(owner.userId(), club.id());
        changeStatus(ownerSession, ownerMembership, "SUSPENDED", 409);
    }

    @Test void mixedCsvImportCommitsOnlyValidRowsAndScopesDuplicateChecksToTheCurrentTenant() throws Exception {
        TokenPair foreignOwner = account();
        ClubSummary foreignClub = clubs.create(foreignOwner.userId(), "Foreign Import Club");
        TokenPair foreignSession = authentication.selectClub(foreignOwner.userId(), foreignOwner.refreshToken(), foreignClub.id());
        importCsv(foreignSession, csv("Email,First,Last,Phone\nshared@example.test,Foreign,Member,\n"), 1, 0);

        TokenPair owner = account();
        ClubSummary club = clubs.create(owner.userId(), "Import Club");
        TokenPair session = authentication.selectClub(owner.userId(), owner.refreshToken(), club.id());
        MockMultipartFile file = csv("""
                Email,First,Last,Phone
                valid@example.test,Valid,Member,0711
                not-an-email,Bad,Email,
                duplicate@example.test,First,Duplicate,
                DUPLICATE@example.test,Second,Duplicate,
                shared@example.test,Shared,Allowed,
                """);

        mvc.perform(multipart("/api/v1/member-imports/preview").file(file)
                        .header("Authorization", bearer(session)).params(mapping()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readyRows").value(2))
                .andExpect(jsonPath("$.invalidRows").value(3));
        importCsv(session, file, 2, 3);
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(session)).param("status", "INVITED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("valid@example.test", "shared@example.test")));

        TokenPair ordinary = account();
        addMember(ordinary, club.id(), "MEMBER");
        TokenPair ordinarySession = authentication.selectClub(ordinary.userId(), ordinary.refreshToken(), club.id());
        mvc.perform(multipart("/api/v1/member-imports/inspect").file(file)
                        .header("Authorization", bearer(ordinarySession)))
                .andExpect(status().isForbidden());
    }

    private void importCsv(TokenPair session, MockMultipartFile file, int invited, int failed) throws Exception {
        mvc.perform(multipart("/api/v1/member-imports/confirm").file(file)
                        .header("Authorization", bearer(session)).params(mapping()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invitedRows").value(invited))
                .andExpect(jsonPath("$.failedRows").value(failed));
    }

    private org.springframework.util.LinkedMultiValueMap<String, String> mapping() {
        var mapping = new org.springframework.util.LinkedMultiValueMap<String, String>();
        mapping.add("emailColumn", "Email"); mapping.add("firstNameColumn", "First");
        mapping.add("lastNameColumn", "Last"); mapping.add("phoneColumn", "Phone");
        return mapping;
    }

    private void changeStatus(TokenPair session, UUID membershipId, String nextStatus, int expectedStatus) throws Exception {
        mvc.perform(patch("/api/v1/members/" + membershipId + "/status")
                        .header("Authorization", bearer(session)).contentType("application/json")
                        .content("{\"status\":\"" + nextStatus + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "members.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private TokenPair account() { return authentication.register(UUID.randomUUID() + "@example.test", "test-password"); }
    private String bearer(TokenPair session) { return "Bearer " + session.accessToken(); }
    private UUID addMember(TokenPair user, UUID club, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into club_memberships(id, club_id, user_id, role_code, created_at) values (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id, club, user.userId(), role);
        return id;
    }
    private UUID membershipId(UUID user, UUID club) {
        return jdbc.queryForObject("select id from club_memberships where user_id = ? and club_id = ?", UUID.class, user, club);
    }
}
