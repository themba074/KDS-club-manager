package com.kds.backend.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clubs")
public class ClubEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "club_type", nullable = false, length = 40) private String clubType;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ClubEntity() {}
    public ClubEntity(UUID id, String name, Instant now) {
        this.id = id;
        this.name = name;
        this.clubType = "INVESTMENT_CLUB";
        this.createdAt = now;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getClubType() { return clubType; }
}
