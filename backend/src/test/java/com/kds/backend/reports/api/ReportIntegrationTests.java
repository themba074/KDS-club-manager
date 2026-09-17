package com.kds.backend.reports.api;

import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ClubService clubs;

    @Test void exportsAreTenantScopedAndPermissionChecked() throws Exception {
        String firstEmail = UUID.randomUUID() + "@example.test";
        var first = auth.register(firstEmail, "test-password");
        var firstClub = clubs.create(first.userId(), "First Private Club");
        var firstSession = auth.selectClub(first.userId(), first.refreshToken(), firstClub.id());
        String secondEmail = UUID.randomUUID() + "@example.test";
        var second = auth.register(secondEmail, "test-password");
        var secondClub = clubs.create(second.userId(), "Second Private Club");
        var secondSession = auth.selectClub(second.userId(), second.refreshToken(), secondClub.id());

        String firstCsv = csv(firstSession);
        String secondCsv = csv(secondSession);
        assertTrue(firstCsv.contains(firstEmail));
        assertFalse(firstCsv.contains(secondEmail));
        assertTrue(secondCsv.contains(secondEmail));
        assertFalse(secondCsv.contains(firstEmail));

        mvc.perform(get("/api/v1/reports/members/export")
                        .header("Authorization", "Bearer " + firstSession.accessToken())
                        .param("from", "2026-12-31").param("to", "2026-01-01").param("format", "CSV"))
                .andExpect(status().isBadRequest());
        mvc.perform(exportRequest(firstSession, "unknown", "CSV")).andExpect(status().isBadRequest());
    }

    @Test void allReportKindsRenderCsvAndPdf() throws Exception {
        var owner = auth.register(UUID.randomUUID() + "@example.test", "test-password");
        var club = clubs.create(owner.userId(), "Reports Club");
        var session = auth.selectClub(owner.userId(), owner.refreshToken(), club.id());
        for (String kind : new String[] {"members", "meetings", "voting"}) {
            MvcResult csv = mvc.perform(exportRequest(session, kind, "CSV")).andExpect(request().asyncStarted()).andReturn();
            mvc.perform(asyncDispatch(csv)).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"));
            MvcResult pdf = mvc.perform(exportRequest(session, kind, "PDF")).andExpect(request().asyncStarted()).andReturn();
            var result = mvc.perform(asyncDispatch(pdf)).andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn();
            assertEquals("%PDF", new String(result.getResponse().getContentAsByteArray(), 0, 4, StandardCharsets.US_ASCII));
        }
    }

    private String csv(TokenPair session) throws Exception {
        MvcResult started = mvc.perform(exportRequest(session, "members", "CSV"))
                .andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(started)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder exportRequest(TokenPair session, String kind, String format) {
        return get("/api/v1/reports/" + kind + "/export")
                .header("Authorization", "Bearer " + session.accessToken())
                .param("from", LocalDate.now().minusYears(1).toString())
                .param("to", LocalDate.now().plusDays(1).toString())
                .param("format", format);
    }
}
