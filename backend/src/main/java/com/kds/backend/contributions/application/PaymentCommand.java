package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record PaymentCommand(UUID scheduleVersionId,UUID membershipId,LocalDate dueDate,BigDecimal amount,
    LocalDate receivedOn,String reference,String note) {}
