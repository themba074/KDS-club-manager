package com.kds.backend.contributions.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record ContributionReminderRequested(UUID clubId,UUID scheduleVersionId,UUID membershipId,String recipientEmail,
        String memberName,String scheduleName,LocalDate dueDate,BigDecimal outstanding,String currency) {}
