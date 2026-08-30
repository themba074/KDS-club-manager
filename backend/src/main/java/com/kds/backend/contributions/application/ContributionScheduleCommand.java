package com.kds.backend.contributions.application;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
public record ContributionScheduleCommand(String name, BigDecimal amount, Frequency frequency,
    LocalDate firstDueDate, LocalDate endDate, LocalDate effectiveFrom,
    AssignmentMode assignmentMode, Set<UUID> membershipIds) {}
