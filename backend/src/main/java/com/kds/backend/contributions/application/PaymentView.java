package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
public record PaymentView(UUID id,UUID scheduleVersionId,String scheduleName,UUID membershipId,String memberName,
    LocalDate dueDate,BigDecimal amount,String currency,LocalDate receivedOn,String reference,String note,
    String proofFileName,Instant createdAt) {}
