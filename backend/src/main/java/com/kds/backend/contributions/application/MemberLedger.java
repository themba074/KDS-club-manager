package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record MemberLedger(UUID membershipId,LocalDate from,LocalDate to,BigDecimal totalExpected,BigDecimal totalPaid,
    BigDecimal balance,String currency,List<LedgerLine> lines) {}
