package com.kds.backend.voting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "motion_result_option_counts")
public class MotionResultOptionEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "motion_id", nullable = false) private UUID motionId;
    @Column(name = "option_id", nullable = false) private UUID optionId;
    @Column(nullable = false) private int position;
    @Column(nullable = false, length = 200) private String label;
    @Column(name = "vote_count", nullable = false) private int voteCount;

    protected MotionResultOptionEntity() {}

    public MotionResultOptionEntity(UUID id, UUID clubId, UUID motionId, UUID optionId,
                                    int position, String label, int voteCount) {
        this.id = id;
        this.clubId = clubId;
        this.motionId = motionId;
        this.optionId = optionId;
        this.position = position;
        this.label = label;
        this.voteCount = voteCount;
    }

    public UUID getOptionId() { return optionId; }
    public int getPosition() { return position; }
    public String getLabel() { return label; }
    public int getVoteCount() { return voteCount; }
}
