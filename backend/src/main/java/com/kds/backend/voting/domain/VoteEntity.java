package com.kds.backend.voting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "votes")
public class VoteEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "motion_id", nullable = false) private UUID motionId;
    @Column(name = "option_id", nullable = false) private UUID optionId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(name = "cast_at", nullable = false) private Instant castAt;

    protected VoteEntity() {}

    public VoteEntity(UUID id, UUID clubId, UUID motionId, UUID optionId, UUID membershipId, Instant castAt) {
        this.id = id;
        this.clubId = clubId;
        this.motionId = motionId;
        this.optionId = optionId;
        this.membershipId = membershipId;
        this.castAt = castAt;
    }

    public UUID getMotionId() { return motionId; }
    public UUID getOptionId() { return optionId; }
    public UUID getMembershipId() { return membershipId; }
    public Instant getCastAt() { return castAt; }
}
