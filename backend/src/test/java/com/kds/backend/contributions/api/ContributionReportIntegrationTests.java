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
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ContributionReportIntegrationTests {
    @Autowired MockMvc mvc;@Autowired AuthService auth;@Autowired ClubService clubs;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
    @Test void summaryAndExportsShareTenantScopedFigures()throws Exception{
        var owner=account();var club=clubs.create(owner.userId(),"Reporting Club");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());var member=account();UUID memberId=addMember(member,club.id());LocalDate today=LocalDate.now();
        UUID versionId=createSchedule(ownerSession,memberId,today);recordPayment(ownerSession,versionId,memberId,today,"40.05");
        mvc.perform(report(ownerSession,"summary",today)).andExpect(status().isOk()).andExpect(jsonPath("$.totalExpected").value(100.10)).andExpect(jsonPath("$.totalCollected").value(40.05)).andExpect(jsonPath("$.totalOutstanding").value(60.05)).andExpect(jsonPath("$.members[0].membershipId").value(memberId.toString()));
        MvcResult csv=startExport(ownerSession,today,"CSV");mvc.perform(asyncDispatch(csv)).andExpect(status().isOk()).andExpect(header().string("Content-Disposition",containsString("contributions-"))).andExpect(content().contentTypeCompatibleWith("text/csv")).andExpect(content().string(containsString("100.10,40.05,60.05,ZAR")));
        MvcResult pdf=startExport(ownerSession,today,"PDF");MvcResult pdfResult=mvc.perform(asyncDispatch(pdf)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn();assertEquals("%PDF",new String(pdfResult.getResponse().getContentAsByteArray(),0,4,StandardCharsets.US_ASCII));
    }
    @Test void reportPermissionAndTenantPredicatesPreventDisclosure()throws Exception{
        var owner=account();var club=clubs.create(owner.userId(),"Private Club");var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());var member=account();UUID memberId=addMember(member,club.id());LocalDate today=LocalDate.now();createSchedule(session,memberId,today);
        var memberSession=auth.selectClub(member.userId(),member.refreshToken(),club.id());mvc.perform(report(memberSession,"summary",today)).andExpect(status().isForbidden());
        var foreign=account();var foreignClub=clubs.create(foreign.userId(),"Other Club");var foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub.id());mvc.perform(report(foreignSession,"summary",today)).andExpect(status().isOk()).andExpect(jsonPath("$.totalExpected").value(0)).andExpect(jsonPath("$.members").isEmpty());
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(TokenPair session,String suffix,LocalDate date){return get("/api/v1/contribution-reports/"+suffix).header("Authorization",bearer(session)).param("from",date.toString()).param("to",date.plusDays(20).toString());}
    private MvcResult startExport(TokenPair session,LocalDate date,String format)throws Exception{return mvc.perform(report(session,"export",date).param("format",format)).andExpect(request().asyncStarted()).andReturn();}
    private UUID createSchedule(TokenPair session,UUID memberId,LocalDate today)throws Exception{Map<String,Object> body=new LinkedHashMap<>();body.put("name","Monthly");body.put("amount","100.10");body.put("frequency","MONTHLY");body.put("firstDueDate",today);body.put("endDate",null);body.put("effectiveFrom",today);body.put("assignmentMode","SELECTED");body.put("membershipIds",Set.of(memberId));String response=mvc.perform(post("/api/v1/contribution-schedules").header("Authorization",bearer(session)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return UUID.fromString(json.readTree(response).get("versionId").asString());}
    private void recordPayment(TokenPair session,UUID version,UUID member,LocalDate due,String amount)throws Exception{Map<String,Object> body=new LinkedHashMap<>();body.put("scheduleVersionId",version);body.put("membershipId",member);body.put("dueDate",due);body.put("amount",amount);body.put("receivedOn",due);body.put("reference","REPORT-1");MockMultipartFile part=new MockMultipartFile("payment","","application/json",json.writeValueAsBytes(body));mvc.perform(multipart("/api/v1/contribution-payments").file(part).header("Authorization",bearer(session))).andExpect(status().isCreated());}
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}
    private UUID addMember(TokenPair user,UUID club){UUID id=UUID.randomUUID();jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",id,club,user.userId(),"MEMBER");return id;}
    private String bearer(TokenPair session){return "Bearer "+session.accessToken();}
}
