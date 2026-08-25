package com.kds.backend.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "replaced_by_hash", length = 64) private String replacedByHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected RefreshTokenEntity() {}
    public RefreshTokenEntity(UUID id, UserEntity user, String tokenHash, UUID familyId, Instant expiresAt, Instant now) {
        this.id = id; this.user = user; this.tokenHash = tokenHash; this.familyId = familyId;
        this.expiresAt = expiresAt; this.createdAt = now;
    }
    public UserEntity getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public UUID getFamilyId() { return familyId; }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public boolean isRotated() { return replacedByHash != null; }
    public void rotate(String replacementHash, Instant now) { revokedAt = now; replacedByHash = replacementHash; }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
}
