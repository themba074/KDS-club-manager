package com.kds.backend.members.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_invitations")
public class MemberInvitationEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "first_name", nullable = false, length = 80) private String firstName;
    @Column(name = "last_name", nullable = false, length = 80) private String lastName;
    @Column(length = 30) private String phone;
    @Column(name = "role_code", nullable = false, length = 40) private String roleCode;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "accepted_at") private Instant acceptedAt;
    @Column(name = "invited_by", nullable = false) private UUID invitedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MemberInvitationEntity() {}

    public MemberInvitationEntity(UUID id, UUID clubId, String email, String firstName, String lastName,
                                  String phone, String roleCode, String tokenHash, Instant expiresAt,
                                  UUID invitedBy, Instant createdAt) {
        this.id = id;
        this.clubId = clubId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.roleCode = roleCode;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getClubId() { return clubId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getRoleCode() { return roleCode; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsableAt(Instant now) { return acceptedAt == null && expiresAt.isAfter(now); }
    public void accept(Instant now) { acceptedAt = now; }
}
