package com.kds.backend.contributions.application;
import java.math.BigDecimal;
import java.util.UUID;
public record MemberContributionSummary(UUID membershipId,String memberName,String memberEmail,BigDecimal expected,
    BigDecimal collected,BigDecimal outstanding,String currency) {}
