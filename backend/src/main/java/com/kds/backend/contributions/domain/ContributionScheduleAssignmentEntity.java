package com.kds.backend.contributions.domain;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="contribution_schedule_assignments")
public class ContributionScheduleAssignmentEntity {
    @EmbeddedId private ContributionScheduleAssignmentId id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    protected ContributionScheduleAssignmentEntity() {}
    public ContributionScheduleAssignmentEntity(UUID versionId,UUID clubId,UUID membershipId){this.id=new ContributionScheduleAssignmentId(versionId,membershipId);this.clubId=clubId;}
    public UUID getClubId(){return clubId;} public UUID getMembershipId(){return id.membershipId();}
}
