package com.kds.backend.contributions.application;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity.*;
import com.kds.backend.members.application.ContributionMember;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public record ContributionScheduleView(UUID scheduleId, UUID versionId, int versionNumber, String name,
    BigDecimal amount, String currency, Frequency frequency, LocalDate firstDueDate, LocalDate endDate,
    LocalDate effectiveFrom, LocalDate effectiveTo, AssignmentMode assignmentMode,
    List<ContributionMember> assignedMembers) {}
