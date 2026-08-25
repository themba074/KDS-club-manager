package com.kds.backend.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {}

    public UserEntity(UUID id, String email, String passwordHash, Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }
}
