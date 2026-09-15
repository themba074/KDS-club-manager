package com.kds.backend.audit.api;

import com.kds.backend.audit.application.AuditLogPage;
import com.kds.backend.audit.application.AuditLogService;
import com.kds.backend.audit.domain.AuditAction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-log")
public class AuditLogController {
    private final AuditLogService service;

    public AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public AuditLogPage search(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(required = false) UUID actor,
                               @RequestParam(required = false) AuditAction action,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size) {
        return service.search(UUID.fromString(jwt.getSubject()), actor, action, from, to, page, size);
    }
}
