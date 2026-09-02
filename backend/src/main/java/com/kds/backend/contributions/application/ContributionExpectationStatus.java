package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record ContributionExpectationStatus(UUID scheduleVersionId,String scheduleName,UUID membershipId,String memberName,
    LocalDate dueDate,BigDecimal expected,BigDecimal paid,BigDecimal outstanding,String currency) {}
