package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.PaymentCommand;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class ContributionPaymentDtos {
    private ContributionPaymentDtos(){}
    public record PaymentRequest(@NotNull UUID scheduleVersionId,@NotNull UUID membershipId,@NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotNull LocalDate receivedOn,
        @Size(max=120) String reference,@Size(max=500) String note){
        public PaymentCommand command(){return new PaymentCommand(scheduleVersionId,membershipId,dueDate,amount,receivedOn,reference,note);}
    }
}
