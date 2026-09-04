package com.kds.backend.meetings.api;

import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.application.MeetingNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MeetingParticipationIntegrationTests {
    @Autowired MockMvc mvc; @Autowired AuthService auth; @Autowired ClubService clubs; @Autowired JdbcTemplate jdbc;
    @MockitoBean MeetingNotificationPublisher notifications;

    @Test void rsvpUsesCurrentMembershipAndOnlyManagersReceiveCounts() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"RSVP Club");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());UUID meeting=meeting(club.id(),owner.userId(),OffsetDateTime.now().plusDays(1));
        mvc.perform(put(url(meeting,"rsvp")).header("Authorization",bearer(ownerSession)).contentType("application/json").content("{\"response\":\"YES\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.response").value("YES")).andExpect(jsonPath("$.counts.yes").value(1));
        var member=account();addMember(member,club.id(),"MEMBER");var memberSession=auth.selectClub(member.userId(),member.refreshToken(),club.id());
        mvc.perform(put(url(meeting,"rsvp")).header("Authorization",bearer(memberSession)).contentType("application/json").content("{\"response\":\"MAYBE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.response").value("MAYBE")).andExpect(jsonPath("$.counts").doesNotExist());
        mvc.perform(get(url(meeting,"rsvp")).header("Authorization",bearer(ownerSession))).andExpect(status().isOk()).andExpect(jsonPath("$.counts.yes").value(1)).andExpect(jsonPath("$.counts.maybe").value(1));
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("select count(*) from meeting_rsvps where club_id=? and meeting_id=?",Integer.class,club.id(),meeting));
    }

    @Test void draftMinutesStayPrivateUntilExplicitPublication() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"Minutes Club");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());UUID meeting=meeting(club.id(),owner.userId(),OffsetDateTime.now().minusDays(1));
        mvc.perform(put(url(meeting,"minutes")).header("Authorization",bearer(ownerSession)).contentType("application/json").content("{\"version\":0,\"body\":\"Decisions\\n- Approve budget\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));
        var member=account();addMember(member,club.id(),"MEMBER");var memberSession=auth.selectClub(member.userId(),member.refreshToken(),club.id());
        mvc.perform(get(url(meeting,"minutes")).header("Authorization",bearer(memberSession))).andExpect(status().isForbidden());
        mvc.perform(post(url(meeting,"minutes/publish")).header("Authorization",bearer(ownerSession)).contentType("application/json").content("{\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true));
        mvc.perform(get(url(meeting,"minutes")).header("Authorization",bearer(memberSession))).andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Decisions\n- Approve budget"));
        mvc.perform(put(url(meeting,"minutes")).header("Authorization",bearer(ownerSession)).contentType("application/json").content("{\"version\":1,\"body\":\"Revised decisions\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));
        mvc.perform(get(url(meeting,"minutes")).header("Authorization",bearer(memberSession))).andExpect(status().isForbidden());
    }

    @Test void attachmentAndEveryParticipationReadRemainTenantScoped() throws Exception {
        var owner=account();var club=clubs.create(owner.userId(),"Files Club");var ownerSession=auth.selectClub(owner.userId(),owner.refreshToken(),club.id());UUID past=meeting(club.id(),owner.userId(),OffsetDateTime.now().minusDays(1));
        var file=new MockMultipartFile("file","minutes.txt","text/plain","approved".getBytes());
        mvc.perform(multipart(url(past,"minutes/attachment")).file(file).param("version","0").header("Authorization",bearer(ownerSession))).andExpect(status().isOk()).andExpect(jsonPath("$.attachmentName").value("minutes.txt"));
        mvc.perform(post(url(past,"minutes/publish")).header("Authorization",bearer(ownerSession)).contentType("application/json").content("{\"version\":0}" )).andExpect(status().isOk());
        mvc.perform(get(url(past,"minutes/attachment")).header("Authorization",bearer(ownerSession))).andExpect(status().isOk()).andExpect(content().bytes("approved".getBytes()));
        var foreign=account();var foreignClub=clubs.create(foreign.userId(),"Foreign");var foreignSession=auth.selectClub(foreign.userId(),foreign.refreshToken(),foreignClub.id());
        mvc.perform(get(url(past,"minutes")).header("Authorization",bearer(foreignSession))).andExpect(status().isForbidden());
        mvc.perform(get(url(past,"rsvp")).header("Authorization",bearer(foreignSession))).andExpect(status().isForbidden());
    }

    private UUID meeting(UUID club,UUID actor,OffsetDateTime startsAt){UUID id=UUID.randomUUID();jdbc.update("insert into meetings(id,club_id,title,starts_at,utc_offset_minutes,duration_minutes,location,created_by,created_at,updated_at,version) values(?,?,?,?,?,?,?, ?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",id,club,"Meeting",startsAt.toInstant(),startsAt.getOffset().getTotalSeconds()/60,60,"Hall",actor);return id;}
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}
    private void addMember(TokenPair user,UUID club,String role){jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",UUID.randomUUID(),club,user.userId(),role);}
    private static String url(UUID meeting,String suffix){return "/api/v1/meetings/"+meeting+"/"+suffix;}
    private static String bearer(TokenPair token){return "Bearer "+token.accessToken();}
}
