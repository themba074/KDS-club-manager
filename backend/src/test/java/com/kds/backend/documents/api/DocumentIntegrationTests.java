package com.kds.backend.documents.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class DocumentIntegrationTests {
    @Autowired MockMvc mvc;@Autowired AuthService auth;@Autowired ClubService clubs;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;

    @Test void uploadUpdateAndReplacementKeepEveryDownloadableVersion()throws Exception{Fixture fixture=fixture();JsonNode created=create(fixture.session(),"Rules","Policy",List.of("MEMBER"),"rules.txt","first",201);UUID id=uuid(created,"id"),first=uuid(created.get("versions").get(0),"id");
        mvc.perform(get("/api/v1/documents/{id}/versions/{version}/download",id,first).header("Authorization",bearer(fixture.session()))).andExpect(status().isOk()).andExpect(content().string("first"));
        String replaced=mvc.perform(multipart("/api/v1/documents/{id}/versions",id).file(file("rules-v2.txt","second")).param("version","0").header("Authorization",bearer(fixture.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.versions.length()").value(2)).andReturn().getResponse().getContentAsString();
        JsonNode updated=json.readTree(replaced);UUID second=uuid(updated.get("versions").get(0),"id");mvc.perform(get("/api/v1/documents/{id}/versions/{version}/download",id,second).header("Authorization",bearer(fixture.session()))).andExpect(status().isOk()).andExpect(content().string("second"));
        mvc.perform(get("/api/v1/documents/{id}/versions/{version}/download",id,first).header("Authorization",bearer(fixture.session()))).andExpect(status().isOk()).andExpect(content().string("first"));
        mvc.perform(put("/api/v1/documents/{id}",id).header("Authorization",bearer(fixture.session())).contentType("application/json").content(metadata(1,"Constitution","Governance",List.of("MEMBER")))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Constitution")).andExpect(jsonPath("$.version").value(2));
        assertEquals(2,jdbc.queryForObject("select count(*) from document_versions where club_id=? and document_id=?",Integer.class,fixture.club(),id));}

    @Test void roleVisibilityAndManagementPermissionsAreEnforced()throws Exception{Fixture fixture=fixture();JsonNode hidden=create(fixture.session(),"Treasury","Finance",List.of("TREASURER"),"budget.csv","a,b",201);UUID id=uuid(hidden,"id"),version=uuid(hidden.get("versions").get(0),"id");TokenPair member=account();addMember(member,fixture.club(),"MEMBER");TokenPair memberSession=select(member,fixture.club());
        mvc.perform(get("/api/v1/documents").header("Authorization",bearer(memberSession))).andExpect(status().isOk()).andExpect(jsonPath("$.documents").isEmpty()).andExpect(jsonPath("$.canManage").value(false)).andExpect(jsonPath("$.visibilityRoles").isEmpty());
        mvc.perform(get("/api/v1/documents/{id}/versions/{version}/download",id,version).header("Authorization",bearer(memberSession))).andExpect(status().isForbidden());create(memberSession,"Denied","Policy",List.of("MEMBER"),"x.txt","x",403);
        TokenPair treasurer=account();addMember(treasurer,fixture.club(),"TREASURER");TokenPair treasurerSession=select(treasurer,fixture.club());mvc.perform(get("/api/v1/documents").header("Authorization",bearer(treasurerSession))).andExpect(status().isOk()).andExpect(jsonPath("$.documents[0].title").value("Treasury"));}

    @Test void everyTargetedEndpointAndRepositoryQueryIsTenantScoped()throws Exception{Fixture owner=fixture();JsonNode document=create(owner.session(),"Private","Policy",List.of("MEMBER"),"private.txt","secret",201);UUID id=uuid(document,"id"),version=uuid(document.get("versions").get(0),"id");Fixture foreign=fixture();
        mvc.perform(get("/api/v1/documents").header("Authorization",bearer(foreign.session()))).andExpect(status().isOk()).andExpect(jsonPath("$.documents").isEmpty());
        mvc.perform(put("/api/v1/documents/{id}",id).header("Authorization",bearer(foreign.session())).contentType("application/json").content(metadata(0,"Stolen","Policy",List.of("MEMBER")))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/documents/{id}/versions",id).file(file("stolen.txt","x")).param("version","0").header("Authorization",bearer(foreign.session()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/documents/{id}/versions/{version}/download",id,version).header("Authorization",bearer(foreign.session()))).andExpect(status().isForbidden());assertEquals("Private",jdbc.queryForObject("select title from club_documents where club_id=? and id=?",String.class,owner.club(),id));}

    private record Fixture(UUID club,TokenPair session){}
    private Fixture fixture(){TokenPair account=account();UUID club=clubs.create(account.userId(),"Documents Club").id();return new Fixture(club,select(account,club));}
    private JsonNode create(TokenPair session,String title,String category,List<String> roles,String name,String content,int status)throws Exception{var metadata=new MockMultipartFile("metadata","metadata.json","application/json",metadata(0,title,category,roles).getBytes(StandardCharsets.UTF_8));String body=mvc.perform(multipart("/api/v1/documents").file(metadata).file(file(name,content)).header("Authorization",bearer(session))).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();return body.isBlank()?null:json.readTree(body);}
    private String metadata(long version,String title,String category,List<String> roles)throws Exception{return json.writeValueAsString(Map.of("version",version,"title",title,"category",category,"visibleRoleCodes",roles));}
    private MockMultipartFile file(String name,String content){return new MockMultipartFile("file",name,name.endsWith(".csv")?"text/csv":"text/plain",content.getBytes(StandardCharsets.UTF_8));}
    private TokenPair account(){return auth.register(UUID.randomUUID()+"@example.test","test-password");}private TokenPair select(TokenPair account,UUID club){return auth.selectClub(account.userId(),account.refreshToken(),club);}private static String bearer(TokenPair pair){return "Bearer "+pair.accessToken();}
    private void addMember(TokenPair account,UUID club,String role){jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",UUID.randomUUID(),club,account.userId(),role);}private static UUID uuid(JsonNode node,String field){return UUID.fromString(node.get(field).asString());}
}
