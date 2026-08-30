package com.kds.backend.contributions.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "contribution_schedule_versions")
public class ContributionScheduleVersionEntity {
    public enum Frequency { MONTHLY, ONCE_OFF }
    public enum AssignmentMode { ALL_CURRENT, SELECTED }
    @Id private UUID id;
    @Column(name = "schedule_id", nullable = false) private UUID scheduleId;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "version_number", nullable = false) private int versionNumber;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Frequency frequency;
    @Column(name = "first_due_date", nullable = false) private LocalDate firstDueDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_mode", nullable = false, length = 20) private AssignmentMode assignmentMode;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected ContributionScheduleVersionEntity() {}
    public ContributionScheduleVersionEntity(UUID id, UUID scheduleId, UUID clubId, int versionNumber, String name,
            BigDecimal amount, Frequency frequency, LocalDate firstDueDate, LocalDate endDate, LocalDate effectiveFrom,
            AssignmentMode assignmentMode, UUID createdBy, Instant createdAt) {
        this.id=id; this.scheduleId=scheduleId; this.clubId=clubId; this.versionNumber=versionNumber; this.name=name;
        this.amount=amount; this.currency="ZAR"; this.frequency=frequency; this.firstDueDate=firstDueDate;
        this.endDate=endDate; this.effectiveFrom=effectiveFrom; this.assignmentMode=assignmentMode;
        this.createdBy=createdBy; this.createdAt=createdAt;
    }
    public void endOn(LocalDate date) { if (effectiveTo != null) throw new IllegalStateException("Revision is already closed."); effectiveTo=date; }
    public UUID getId(){return id;} public UUID getScheduleId(){return scheduleId;} public UUID getClubId(){return clubId;}
    public int getVersionNumber(){return versionNumber;} public String getName(){return name;} public BigDecimal getAmount(){return amount;}
    public String getCurrency(){return currency;} public Frequency getFrequency(){return frequency;} public LocalDate getFirstDueDate(){return firstDueDate;}
    public LocalDate getEndDate(){return endDate;} public LocalDate getEffectiveFrom(){return effectiveFrom;} public LocalDate getEffectiveTo(){return effectiveTo;}
    public AssignmentMode getAssignmentMode(){return assignmentMode;}
}
