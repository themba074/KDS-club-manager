package com.kds.backend.identity.infrastructure;

import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

/** Applied after bearer-token verification and before any tenant application code. */
public class TenantContextFilter extends OncePerRequestFilter {
    private final ClubService clubs;
    public TenantContextFilter(ClubService clubs) { this.clubs = clubs; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantContext.clear();
        try {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            boolean tenantRequest = path.startsWith("/api/v1/") && !path.startsWith("/api/v1/auth/")
                    && !path.equals("/api/v1/clubs");
            if (tenantRequest && SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
                try {
                    String claim = authentication.getToken().getClaimAsString("clubId");
                    if (claim == null) throw new AccessDeniedException("No active club");
                    UUID clubId = UUID.fromString(claim);
                    var club = clubs.requireMembership(UUID.fromString(authentication.getName()), clubId);
                    TenantContext.set(clubId);
                    var authorities = club.permissions().stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new JwtAuthenticationToken(authentication.getToken(), authorities, authentication.getName()));
                } catch (IllegalArgumentException | AccessDeniedException exception) {
                    response.setStatus(403);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Select a club you belong to before accessing club data.\"}");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
