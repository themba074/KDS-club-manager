package com.kds.backend.audit.application;

import com.kds.backend.audit.domain.AuditAction;
import java.time.Instant;
import java.util.UUID;

public record AuditLogView(UUID id, UUID actorId, AuditAction action, String entityType, UUID entityId,
                           String previousValue, String newValue, Instant occurredAt) {}
