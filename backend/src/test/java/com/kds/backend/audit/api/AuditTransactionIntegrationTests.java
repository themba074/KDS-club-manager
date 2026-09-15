package com.kds.backend.audit.api;

import com.kds.backend.audit.repository.AuditLogRepository;
import com.kds.backend.identity.application.AuthService;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.RoleAssignmentService;
import com.kds.backend.identity.application.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest @ActiveProfiles("test")
class AuditTransactionIntegrationTests {
    @Autowired AuthService auth;
    @Autowired ClubService clubs;
    @Autowired RoleAssignmentService roles;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AuditLogRepository auditRepository;

    @Test void auditWriteFailureRollsBackTheRoleChange() {
        var owner = auth.register(UUID.randomUUID() + "@example.test", "test-password");
        UUID clubId = clubs.create(owner.userId(), "Rollback Club").id();
        var member = auth.register(UUID.randomUUID() + "@example.test", "test-password");
        UUID membershipId = UUID.randomUUID();
        jdbc.update("insert into club_memberships(id,club_id,user_id,role_code,status,created_at) values(?,?,?,?, 'ACTIVE',CURRENT_TIMESTAMP)",
                membershipId, clubId, member.userId(), "MEMBER");
        doThrow(new IllegalStateException("audit storage unavailable")).when(auditRepository).append(any());
        TenantContext.set(clubId);
        try {
            assertThrows(IllegalStateException.class,
                    () -> roles.assign(owner.userId(), membershipId, "TREASURER"));
        } finally {
            TenantContext.clear();
        }
        assertEquals("MEMBER", jdbc.queryForObject("select role_code from club_memberships where id=? and club_id=?",
                String.class, membershipId, clubId));
    }
}
