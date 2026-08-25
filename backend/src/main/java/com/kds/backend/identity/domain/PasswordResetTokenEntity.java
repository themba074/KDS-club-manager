package com.kds.backend.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PasswordResetTokenEntity() {}
    public PasswordResetTokenEntity(UUID id, UserEntity user, String tokenHash, Instant expiresAt, Instant now) {
        this.id = id; this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt; this.createdAt = now;
    }
    public UserEntity getUser() { return user; }
    public boolean isUsableAt(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public void markUsed(Instant now) { usedAt = now; }
}
