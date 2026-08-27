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
    @Column(name = "role_code", nullable = false) private String roleCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ClubMembershipEntity() {}
    public ClubMembershipEntity(UUID id, ClubEntity club, UUID userId, boolean administrator, Instant now) {
        this.id = id;
        this.club = club;
        this.userId = userId;
        this.roleCode = administrator ? "ADMINISTRATOR" : "MEMBER";
        this.createdAt = now;
    }
    public ClubEntity getClub() { return club; }
    public boolean isAdministrator() { return "ADMINISTRATOR".equals(roleCode); }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getRoleCode() { return roleCode; }
    public void assignRole(String roleCode) { this.roleCode = roleCode; }
}
