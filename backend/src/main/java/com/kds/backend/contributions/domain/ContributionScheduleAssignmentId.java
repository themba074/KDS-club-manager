package com.kds.backend.contributions.domain;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
@Embeddable
public record ContributionScheduleAssignmentId(@Column(name="schedule_version_id") UUID scheduleVersionId,
                                                @Column(name="membership_id") UUID membershipId) implements Serializable {
    public ContributionScheduleAssignmentId { }
}
