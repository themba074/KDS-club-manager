package com.kds.backend.audit.api;

import com.kds.backend.audit.application.AuditLogService;
import com.kds.backend.audit.domain.AuditAction;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.identity.application.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuditLogIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuditLogService audit;

    @Test void administratorFiltersAppendOnlyEntriesAndForeignClubCannotSeeThem() throws Exception {
        TokenPair owner=account(); UUID club=clubs.create(owner.userId(),"Audited Club").id();
        TokenPair session=auth.selectClub(owner.userId(),owner.refreshToken(),club);
        TokenPair member=account();UUID memberId=addMember(member,club,"MEMBER");
        mvc.perform(put("/api/v1/role-members/{id}",memberId).header("Authorization",bearer(session))
                .contentType("application/json").content("{\"roleCode\":\"TREASURER\"}"))
                .andExpect(status().isNoContent());
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_log where club_id=? and action='ROLE_ASSIGNED' and entity_id=?",Integer.class,club,memberId));

        LocalDate today=LocalDate.now(ZoneOffset.UTC);
        mvc.perform(get("/api/v1/audit-log").header("Authorization",bearer(session)).param("actor",owner.userId().toString())
                .param("action",AuditAction.ROLE_ASSIGNED.name()).param("from",today.minusDays(1).toString())
                .param("to",today.plusDays(1).toString()).param("page","0").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(owner.userId().toString()))
                .andExpect(jsonPath("$.content[0].previousValue").value("MEMBER"))
                .andExpect(jsonPath("$.content[0].newValue").value("TREASURER"));
        mvc.perform(get("/api/v1/audit-log").header("Authorization",bearer(session)).param("actor",member.userId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/audit-log").header("Authorization",bearer(session)).param("size","101"))
                .andExpect(status().isBadRequest());

        TokenPair foreign=account();UUID foreignClub=clubs.create(foreign.userId(),"Foreign Club").id();
        TokenPair foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub);
        mvc.perform(get("/api/v1/audit-log").header("Authorization",bearer(foreignSession)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertEquals(0,jdbc.queryForObject("select count(*) from audit_log where club_id=?",Integer.class,foreignClub));
    }

    @Test void chairpersonAndMemberCannotReadAuditLogAndWritesRequireAnExistingTransaction() throws Exception {
        TokenPair owner=account();UUID club=clubs.create(owner.userId(),"Permissions Club").id();
        TokenPair chairperson=account();addMember(chairperson,club,"CHAIRPERSON");
        TokenPair member=account();addMember(member,club,"MEMBER");
        for(TokenPair account:new TokenPair[]{chairperson,member}){
            TokenPair session=auth.selectClub(account.userId(),account.refreshToken(),club);
            assertFalse(session.activeClub().permissions().contains("AUDIT_READ"));
            mvc.perform(get("/api/v1/audit-log").header("Authorization",bearer(session))).andExpect(status().isForbidden());
        }
        TokenPair ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club);
        TenantContext.set(club);
        try { assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                ()->audit.record(owner.userId(),AuditAction.ROLE_ASSIGNED,"MEMBERSHIP",UUID.randomUUID(),"MEMBER","TREASURER")); }
        finally { TenantContext.clear(); }
        mvc.perform(post("/api/v1/audit-log").header("Authorization",bearer(ownerSession))).andExpect(status().isMethodNotAllowed());
    }

    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}
    private UUID addMember(TokenPair user,UUID club,String role){UUID id=UUID.randomUUID();jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",id,club,user.userId(),role);return id;}
    private static String bearer(TokenPair session){return "Bearer "+session.accessToken();}
}
