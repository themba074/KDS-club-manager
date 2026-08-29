package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record ExpectedContribution(UUID scheduleId, UUID scheduleVersionId, String scheduleName,
    UUID membershipId, String memberEmail, String memberName, LocalDate dueDate, BigDecimal amount, String currency) {}
