package com.kds.backend.contributions.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "contribution_schedules")
public class ContributionScheduleEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected ContributionScheduleEntity() {}
    public ContributionScheduleEntity(UUID id, UUID clubId, UUID createdBy, Instant createdAt) {
        this.id = id; this.clubId = clubId; this.createdBy = createdBy; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getClubId() { return clubId; }
}
