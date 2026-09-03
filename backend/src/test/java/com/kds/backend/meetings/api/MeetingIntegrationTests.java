package com.kds.backend.meetings.api;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.application.MeetingNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MeetingIntegrationTests {
    @Autowired MockMvc mvc;@Autowired AuthService auth;@Autowired ClubService clubs;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
    @MockitoBean MeetingNotificationPublisher notifications;
    @Test void createEditAndListKeepAgendaOrderedAndNotifyAffectedMembers() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"Governance Club");var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        String created=create(session,request(0,"Committee meeting",OffsetDateTime.now().plusDays(2),List.of("Opening","Finance")),201);
        var node=json.readTree(created);UUID id=UUID.fromString(node.get("id").asString());
        mvc.perform(get("/api/v1/meetings").header("Authorization",bearer(session)).param("view","UPCOMING")).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Committee meeting")).andExpect(jsonPath("$[0].agendaItems[0].position").value(0)).andExpect(jsonPath("$[0].agendaItems[1].position").value(1));
        mvc.perform(put("/api/v1/meetings/"+id).header("Authorization",bearer(session)).contentType("application/json").content(request(0,"Updated meeting",OffsetDateTime.now().plusDays(3),List.of("Decisions"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.agendaItems.length()").value(1)).andExpect(jsonPath("$.agendaItems[0].title").value("Decisions"));
        verify(notifications,times(2)).publish(any());
        mvc.perform(put("/api/v1/meetings/"+id).header("Authorization",bearer(session)).contentType("application/json").content(request(0,"Stale",OffsetDateTime.now().plusDays(4),List.of("Stale")))).andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("select count(*) from meeting_agenda_items where club_id=? and meeting_id=?",Integer.class,club.id(),id));
    }
    @Test void tenantAndPermissionsPreventLeaksWhileMembersCanRead() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"First");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        UUID id=UUID.fromString(json.readTree(create(ownerSession,request(0,"Private meeting",OffsetDateTime.now().plusDays(2),List.of("Item")),201)).get("id").asString());
        var foreign=account();var foreignClub=clubs.create(foreign.userId(),"Foreign");var foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub.id());
        mvc.perform(get("/api/v1/meetings").header("Authorization",bearer(foreignSession))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(put("/api/v1/meetings/"+id).header("Authorization",bearer(foreignSession)).contentType("application/json").content(request(0,"Foreign",OffsetDateTime.now().plusDays(3),List.of("Item")))).andExpect(status().isForbidden());
        var member=account();addMember(member,club.id(),"MEMBER");var memberSession=auth.selectClub(member.userId(),member.refreshToken(),club.id());
        mvc.perform(get("/api/v1/meetings").header("Authorization",bearer(memberSession))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        create(memberSession,request(0,"Denied",OffsetDateTime.now().plusDays(2),List.of("Item")),403);
    }
    @Test void validationAndNotificationFailureDoNotCorruptMeetingWrites() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"Validation");var session=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());
        create(session,request(0,"No agenda",OffsetDateTime.now().plusDays(1),List.of()),400);
        Map<String,Object> noPlace=json.readValue(request(0,"No place",OffsetDateTime.now().plusDays(1),List.of("Item")),Map.class);noPlace.put("location",null);noPlace.put("meetingUrl",null);create(session,json.writeValueAsString(noPlace),400);
        create(session,request(0,"Past",OffsetDateTime.now().minusDays(1),List.of("Item")),400);
        doThrow(new IllegalStateException("notification offline")).when(notifications).publish(any());
        create(session,request(0,"Still saved",OffsetDateTime.now().plusDays(1),List.of("Item")),201);
        mvc.perform(get("/api/v1/meetings").header("Authorization",bearer(session))).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Still saved"));
    }
    private String create(TokenPair session,String body,int status) throws Exception{return mvc.perform(post("/api/v1/meetings").header("Authorization",bearer(session)).contentType("application/json").content(body)).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();}
    private String request(long version,String title,OffsetDateTime start,List<String> agenda){var value=new LinkedHashMap<String,Object>();value.put("version",version);value.put("title",title);value.put("description","Club business");value.put("startsAt",start);value.put("durationMinutes",60);value.put("location","Community hall");value.put("meetingUrl",null);value.put("agendaItems",agenda.stream().map(item->Map.of("title",item,"description","Discuss "+item)).toList());return json.writeValueAsString(value);}
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}private String bearer(TokenPair token){return "Bearer "+token.accessToken();}
    private void addMember(TokenPair user,UUID club,String role){jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",UUID.randomUUID(),club,user.userId(),role);}
}
