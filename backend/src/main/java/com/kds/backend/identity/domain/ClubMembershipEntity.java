package com.kds.backend.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "club_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "user_id"}))
public class ClubMembershipEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false) private ClubEntity club;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private boolean administrator;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ClubMembershipEntity() {}
    public ClubMembershipEntity(UUID id, ClubEntity club, UUID userId, boolean administrator, Instant now) {
        this.id = id;
        this.club = club;
        this.userId = userId;
        this.administrator = administrator;
        this.createdAt = now;
    }
    public ClubEntity getClub() { return club; }
    public boolean isAdministrator() { return administrator; }
}
