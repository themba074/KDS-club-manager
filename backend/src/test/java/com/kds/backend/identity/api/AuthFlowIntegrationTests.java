package com.kds.backend.identity.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTests {
    private final MockMvc mockMvc;

    @Autowired
    AuthFlowIntegrationTests(MockMvc mockMvc) { this.mockMvc = mockMvc; }

    @Test
    void dockerLoopbackOriginIsAllowedByCors() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void registerLoginAndRefreshRotateTheSession() throws Exception {
        String email = "member@example.com";
        String password = "a-secure-password";

        Cookie registrationCookie = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getCookie("kds_refresh_token");

        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());

        Cookie rotatedCookie = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(registrationCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andReturn().getResponse().getCookie("kds_refresh_token");

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(registrationCookie))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }
}
