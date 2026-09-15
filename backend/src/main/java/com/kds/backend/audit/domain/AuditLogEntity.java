package com.kds.backend.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AuditAction action;
    @Column(name = "entity_type", nullable = false, length = 40) private String entityType;
    @Column(name = "entity_id", nullable = false) private UUID entityId;
    @Column(name = "previous_value", length = 80) private String previousValue;
    @Column(name = "new_value", length = 80) private String newValue;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected AuditLogEntity() {}

    public AuditLogEntity(UUID id, UUID clubId, UUID actorId, AuditAction action, String entityType,
                          UUID entityId, String previousValue, String newValue, Instant occurredAt) {
        this.id = id;
        this.clubId = clubId;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getClubId() { return clubId; }
    public UUID getActorId() { return actorId; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public Instant getOccurredAt() { return occurredAt; }
}
