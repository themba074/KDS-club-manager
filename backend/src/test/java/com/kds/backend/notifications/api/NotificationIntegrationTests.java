package com.kds.backend.notifications.api;

import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class NotificationIntegrationTests {
    @Autowired MockMvc mvc;@Autowired AuthService auth;@Autowired ClubService clubs;@Autowired JdbcTemplate jdbc;
    @Test void feedUnreadAndReadOperationsAreMembershipAndTenantScoped()throws Exception{
        Fixture owner=fixture("Owner"),foreign=fixture("Foreign");UUID notification=insert(owner);
        mvc.perform(get("/api/v1/notifications").header("Authorization",bearer(owner.session()))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(notification.toString())).andExpect(jsonPath("$[0].readAt").doesNotExist());
        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization",bearer(owner.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(put("/api/v1/notifications/{id}/read",notification).header("Authorization",bearer(foreign.session()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/notifications").header("Authorization",bearer(foreign.session()))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization",bearer(foreign.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(put("/api/v1/notifications/read-all").header("Authorization",bearer(foreign.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.updated").value(0));
        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization",bearer(owner.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(put("/api/v1/notifications/{id}/read",notification).header("Authorization",bearer(owner.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.readAt").exists());
        mvc.perform(put("/api/v1/notifications/read-all").header("Authorization",bearer(owner.session()))).andExpect(status().isOk());
    }
    private record Fixture(UUID club,UUID membership,TokenPair session){}
    private Fixture fixture(String name){TokenPair account=auth.register(UUID.randomUUID()+"@example.test","test-password");UUID club=clubs.create(account.userId(),name).id();UUID membership=jdbc.queryForObject("select id from club_memberships where club_id=? and user_id=?",UUID.class,club,account.userId());return new Fixture(club,membership,auth.selectClub(account.userId(),account.refreshToken(),club));}
    private UUID insert(Fixture fixture){UUID id=UUID.randomUUID();Instant now=Instant.now();jdbc.update("insert into notifications(id,club_id,membership_id,recipient_email,type,title,message,target_path,source_type,source_id,deduplication_key,available_at,created_at,email_attempts) values(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",id,fixture.club(),fixture.membership(),"member@example.test","MEETING_SCHEDULED","Meeting scheduled","Monthly meeting","/meetings","MEETING",UUID.randomUUID(),UUID.randomUUID().toString(),now,now);return id;}
    private static String bearer(TokenPair pair){return "Bearer "+pair.accessToken();}
}
