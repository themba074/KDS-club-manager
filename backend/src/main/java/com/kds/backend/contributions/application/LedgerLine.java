package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record LedgerLine(String type,LocalDate activityDate,String description,UUID scheduleVersionId,UUID paymentId,
    BigDecimal expected,BigDecimal paid,BigDecimal runningBalance,String currency) {}
