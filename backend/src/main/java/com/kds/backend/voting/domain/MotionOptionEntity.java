package com.kds.backend.voting.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "motion_options")
public class MotionOptionEntity {
    @Id private UUID id;
    @Column(name = "motion_id", insertable = false, updatable = false) private UUID motionId;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(nullable = false) private int position;
    @Column(nullable = false, length = 200) private String label;

    protected MotionOptionEntity() {}
    public MotionOptionEntity(UUID id, UUID motionId, UUID clubId, int position, String label) {
        this.id = id;
        this.motionId = motionId;
        this.clubId = clubId;
        this.position = position;
        this.label = label;
    }
    public void update(String label) { this.label = label; }
    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public String getLabel() { return label; }
}
