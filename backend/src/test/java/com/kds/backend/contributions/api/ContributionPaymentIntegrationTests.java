package com.kds.backend.contributions.api;

import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ContributionPaymentIntegrationTests {
    @Autowired MockMvc mvc; @Autowired AuthService auth; @Autowired ClubService clubs; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json;
    @Test void treasurerRecordsPartialPaymentAndMemberReadsOnlyOwnLedger() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"Ledger Club");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        var member=account();var other=account();UUID memberId=addMember(member,club.id()),otherId=addMember(other,club.id());LocalDate today=LocalDate.now();
        String schedule=createSchedule(ownerSession,today);UUID versionId=UUID.fromString(json.readTree(schedule).get("versionId").asString());
        var payment=paymentPart(versionId,memberId,today,"40.00");var proof=new MockMultipartFile("proof","receipt.pdf","application/pdf",new byte[]{1,2,3});
        mvc.perform(multipart("/api/v1/contribution-payments").file(payment).file(proof).header("Authorization",bearer(ownerSession)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.membershipId").value(memberId.toString())).andExpect(jsonPath("$.proofFileName").value("receipt.pdf"));
        var memberSession=auth.selectClub(member.userId(),member.refreshToken(),club.id());
        mvc.perform(get("/api/v1/contribution-payments/my-ledger").header("Authorization",bearer(memberSession)).param("from",today.toString()).param("to",today.plusDays(20).toString()).param("membershipId",otherId.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.membershipId").value(memberId.toString())).andExpect(jsonPath("$.totalExpected").value(100.00))
            .andExpect(jsonPath("$.totalPaid").value(40.00)).andExpect(jsonPath("$.balance").value(60.00)).andExpect(jsonPath("$.lines.length()").value(2));
        assertEquals(1,jdbc.queryForObject("select count(*) from contribution_payments where club_id=? and membership_id=?",Integer.class,club.id(),memberId));
        assertEquals(0,jdbc.queryForObject("select count(*) from contribution_payments where club_id=? and membership_id=?",Integer.class,club.id(),otherId));
    }
    @Test void tenantPredicatesRejectForeignExpectationAndHidePayments() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"First");var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());var member=account();UUID memberId=addMember(member,club.id());LocalDate today=LocalDate.now();
        UUID versionId=UUID.fromString(json.readTree(createSchedule(session,today)).get("versionId").asString());
        var foreign=account();var foreignClub=clubs.create(foreign.userId(),"Second");var foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub.id());
        mvc.perform(multipart("/api/v1/contribution-payments").file(paymentPart(versionId,memberId,today,"10.00")).header("Authorization",bearer(foreignSession))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/contribution-payments/my-ledger").header("Authorization",bearer(foreignSession)).param("from",today.toString()).param("to",today.plusMonths(1).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalPaid").value(0)).andExpect(jsonPath("$.lines").isEmpty());
    }
    private String createSchedule(TokenPair session,LocalDate today)throws Exception{
        Map<String,Object> body=new LinkedHashMap<>();body.put("name","Monthly");body.put("amount","100.00");body.put("frequency","MONTHLY");body.put("firstDueDate",today);body.put("endDate",null);body.put("effectiveFrom",today);body.put("assignmentMode","ALL_CURRENT");body.put("membershipIds",Set.of());
        return mvc.perform(post("/api/v1/contribution-schedules").header("Authorization",bearer(session)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    private MockMultipartFile paymentPart(UUID version,UUID member,LocalDate due,String amount){Map<String,Object> body=new LinkedHashMap<>();body.put("scheduleVersionId",version);body.put("membershipId",member);body.put("dueDate",due);body.put("amount",amount);body.put("receivedOn",due);body.put("reference","EFT-42");body.put("note","Partial");return new MockMultipartFile("payment","","application/json",json.writeValueAsBytes(body));}
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}
    private UUID addMember(TokenPair user,UUID club){UUID id=UUID.randomUUID();jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",id,club,user.userId(),"MEMBER");return id;}
    private String bearer(TokenPair session){return "Bearer "+session.accessToken();}
}
