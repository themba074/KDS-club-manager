package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.ContributionScheduleCommand;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
public final class ContributionScheduleDtos {
    private ContributionScheduleDtos() {}
    public record ScheduleRequest(@NotBlank @Size(max=120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
        @NotNull Frequency frequency,@NotNull LocalDate firstDueDate,LocalDate endDate,
        @NotNull LocalDate effectiveFrom,@NotNull AssignmentMode assignmentMode,
        @NotNull @Size(max=1000) Set<UUID> membershipIds) {
        public ContributionScheduleCommand command(){return new ContributionScheduleCommand(name,amount,frequency,firstDueDate,endDate,effectiveFrom,assignmentMode,membershipIds);}
    }
}
