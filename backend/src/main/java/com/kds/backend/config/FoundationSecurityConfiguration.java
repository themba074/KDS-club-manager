package com.kds.backend.config;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.infrastructure.TenantContextFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class FoundationSecurityConfiguration {

    @Bean
    SecurityFilterChain foundationSecurityFilterChain(HttpSecurity http, ClubService clubs) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/logout", "/api/v1/auth/password-reset/request", "/api/v1/auth/password-reset/confirm").permitAll()
                .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {}))
                .addFilterAfter(new TenantContextFilter(clubs), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.auth.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
