package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
public record ContributionReport(UUID clubId,String clubName,LocalDate from,LocalDate to,Instant generatedAt,
    BigDecimal totalExpected,BigDecimal totalCollected,BigDecimal totalOutstanding,String currency,
    List<MemberContributionSummary> members) {}
