package com.kds.backend.contributions.api;

import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ContributionScheduleIntegrationTests {
    @Autowired MockMvc mvc; @Autowired AuthService auth; @Autowired ClubService clubs; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json;
    @Test void createReviseAndCalculateUpcomingWithoutMutatingHistory() throws Exception {
        var owner=account(); var club=clubs.create(owner.userId(),"Savings Club"); var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        var member=account(); UUID memberId=addMember(member,club.id(),"MEMBER");
        LocalDate today=LocalDate.now(), first=today.plusDays(1), revisionDate=today.plusMonths(1).withDayOfMonth(1);
        String created=create(session,"Monthly savings","100.00","MONTHLY",first,null,today,"ALL_CURRENT",Set.of(),201);
        UUID scheduleId=UUID.fromString(json.readTree(created).get("scheduleId").asString());
        mvc.perform(get("/api/v1/contribution-schedules/upcoming").header("Authorization",bearer(session))
            .param("from",today.toString()).param("to",today.plusMonths(2).toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.membershipId == '%s')]",memberId).exists()).andExpect(jsonPath("$[*].amount",everyItem(is(100.00))));
        String revision=request("Monthly savings", "125.50","MONTHLY",first,null,revisionDate,"SELECTED",Set.of(memberId));
        mvc.perform(put("/api/v1/contribution-schedules/"+scheduleId).header("Authorization",bearer(session)).contentType("application/json").content(revision))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versionNumber").value(2)).andExpect(jsonPath("$.amount").value(125.50));
        assertEquals(2,jdbc.queryForObject("select count(*) from contribution_schedule_versions where club_id=? and schedule_id=?",Integer.class,club.id(),scheduleId));
        assertEquals(new java.math.BigDecimal("100.00"),jdbc.queryForObject("select amount from contribution_schedule_versions where club_id=? and schedule_id=? and version_number=1",java.math.BigDecimal.class,club.id(),scheduleId));
        assertEquals(revisionDate.minusDays(1),jdbc.queryForObject("select effective_to from contribution_schedule_versions where club_id=? and schedule_id=? and version_number=1",LocalDate.class,club.id(),scheduleId));
        mvc.perform(get("/api/v1/contribution-schedules/upcoming").header("Authorization",bearer(session))
            .param("from",today.toString()).param("to",today.plusMonths(2).toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].amount",hasItems(100.00,125.50)));
    }
    @Test void permissionsAndTenantPredicatesBlockWritesAndCrossClubReads() throws Exception {
        var owner=account(); var club=clubs.create(owner.userId(),"First"); var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        String created=create(ownerSession,"Joining fee","50.00","ONCE_OFF",LocalDate.now().plusDays(2),null,LocalDate.now(),"ALL_CURRENT",Set.of(),201);
        UUID schedule=UUID.fromString(json.readTree(created).get("scheduleId").asString());
        var foreign=account(); var foreignClub=clubs.create(foreign.userId(),"Second"); var foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub.id());
        mvc.perform(get("/api/v1/contribution-schedules").header("Authorization",bearer(foreignSession))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(put("/api/v1/contribution-schedules/"+schedule).header("Authorization",bearer(foreignSession)).contentType("application/json")
            .content(request("Foreign edit","20.00","ONCE_OFF",LocalDate.now().plusDays(3),null,LocalDate.now().plusDays(1),"ALL_CURRENT",Set.of()))).andExpect(status().isForbidden());
        var ordinary=account(); addMember(ordinary,club.id(),"MEMBER"); var memberSession=auth.selectClub(ordinary.userId(),ordinary.refreshToken(),club.id());
        mvc.perform(get("/api/v1/contribution-schedules").header("Authorization",bearer(memberSession))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        create(memberSession,"Denied","10","ONCE_OFF",LocalDate.now().plusDays(1),null,LocalDate.now(),"ALL_CURRENT",Set.of(),403);
        mvc.perform(get("/api/v1/contribution-schedules/assignable-members").header("Authorization",bearer(memberSession))).andExpect(status().isForbidden());
    }
    @Test void validationRejectsBadAmountsDatesRangesAndForeignAssignments() throws Exception {
        var owner=account(); var club=clubs.create(owner.userId(),"Validation"); var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        create(session,"Bad","0","MONTHLY",LocalDate.now().plusDays(1),null,LocalDate.now(),"ALL_CURRENT",Set.of(),400);
        create(session,"Bad","10","ONCE_OFF",LocalDate.now().plusDays(1),LocalDate.now().plusDays(2),LocalDate.now(),"ALL_CURRENT",Set.of(),400);
        create(session,"Bad","10","MONTHLY",LocalDate.now().minusDays(1),null,LocalDate.now(),"ALL_CURRENT",Set.of(),400);
        create(session,"Bad","10","MONTHLY",LocalDate.now().plusDays(1),null,LocalDate.now(),"SELECTED",Set.of(UUID.randomUUID()),403);
        mvc.perform(get("/api/v1/contribution-schedules/upcoming").header("Authorization",bearer(session)).param("from",LocalDate.now().toString()).param("to",LocalDate.now().plusYears(1).plusDays(1).toString())).andExpect(status().isBadRequest());
    }
    private String create(TokenPair s,String name,String amount,String frequency,LocalDate due,LocalDate end,LocalDate effective,String mode,Set<UUID> ids,int status) throws Exception {
        return mvc.perform(post("/api/v1/contribution-schedules").header("Authorization",bearer(s)).contentType("application/json").content(request(name,amount,frequency,due,end,effective,mode,ids)))
            .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }
    private String request(String name,String amount,String frequency,LocalDate due,LocalDate end,LocalDate effective,String mode,Set<UUID> ids){
        var value=new LinkedHashMap<String,Object>(); value.put("name",name); value.put("amount",amount); value.put("frequency",frequency); value.put("firstDueDate",due); value.put("endDate",end); value.put("effectiveFrom",effective); value.put("assignmentMode",mode); value.put("membershipIds",ids); return json.writeValueAsString(value);
    }
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}
    private String bearer(TokenPair s){return "Bearer "+s.accessToken();}
    private UUID addMember(TokenPair user,UUID club,String role){UUID id=UUID.randomUUID();jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",id,club,user.userId(),role);return id;}
}
