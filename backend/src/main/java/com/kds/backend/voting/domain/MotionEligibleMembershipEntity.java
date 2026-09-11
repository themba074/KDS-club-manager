package com.kds.backend.voting.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "motion_eligible_memberships")
public class MotionEligibleMembershipEntity {
    @Id private UUID id;
    @Column(name = "motion_id", insertable = false, updatable = false) private UUID motionId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(name = "club_id", nullable = false) private UUID clubId;

    protected MotionEligibleMembershipEntity() {}
    public MotionEligibleMembershipEntity(UUID motionId, UUID membershipId, UUID clubId) {
        id = UUID.randomUUID();
        this.motionId = motionId;
        this.membershipId = membershipId;
        this.clubId = clubId;
    }
    public UUID getMembershipId() { return membershipId; }
}
