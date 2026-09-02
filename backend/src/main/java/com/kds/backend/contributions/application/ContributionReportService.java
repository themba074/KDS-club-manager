package com.kds.backend.contributions.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.contributions.domain.ContributionPaymentEntity;
import com.kds.backend.contributions.repository.ContributionPaymentRepository;
import com.kds.backend.identity.application.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ContributionReportService {
    private final ContributionScheduleService schedules; private final ContributionPaymentRepository payments;
    private final ClubService clubs; private final Clock clock;
    public ContributionReportService(ContributionScheduleService schedules,ContributionPaymentRepository payments,ClubService clubs,Clock clock){
        this.schedules=schedules;this.payments=payments;this.clubs=clubs;this.clock=clock;
    }
    public ContributionReport summary(UUID actor,LocalDate from,LocalDate to){
        ClubSummary club=require(actor); List<ExpectedContribution> expected=schedules.expectationsForReport(from,to);
        List<ContributionPaymentEntity> collected=payments.forPeriod(from,to); Map<UUID,MutableMember> members=new HashMap<>();
        for(ExpectedContribution item:expected)members.computeIfAbsent(item.membershipId(),id->new MutableMember(id,item.memberName(),item.memberEmail(),item.currency())).addExpected(item.amount());
        for(ContributionPaymentEntity payment:collected)members.computeIfAbsent(payment.getMembershipId(),id->new MutableMember(id,"Former member","Former member",payment.getCurrency())).addCollected(payment.getAmount());
        List<MemberContributionSummary> rows=members.values().stream().map(MutableMember::view)
            .sorted(Comparator.comparing(MemberContributionSummary::memberName,String.CASE_INSENSITIVE_ORDER).thenComparing(MemberContributionSummary::membershipId)).toList();
        BigDecimal totalExpected=rows.stream().map(MemberContributionSummary::expected).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal totalCollected=rows.stream().map(MemberContributionSummary::collected).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal totalOutstanding=rows.stream().map(MemberContributionSummary::outstanding).reduce(BigDecimal.ZERO,BigDecimal::add);
        String currency=rows.stream().map(MemberContributionSummary::currency).findFirst().orElse("ZAR");
        return new ContributionReport(club.id(),club.name(),from,to,clock.instant(),totalExpected,totalCollected,totalOutstanding,currency,rows);
    }
    private ClubSummary require(UUID actor){ClubSummary club=clubs.requireMembership(actor,TenantContext.requireClubId());if(!club.permissions().contains(Permission.REPORTS_READ.name()))throw new AccessDeniedException("You do not have permission for this report.");return club;}
    private static final class MutableMember {
        private final UUID id; private final String name,email,currency; private BigDecimal expected=BigDecimal.ZERO,collected=BigDecimal.ZERO;
        private MutableMember(UUID id,String name,String email,String currency){this.id=id;this.name=name;this.email=email;this.currency=currency;}
        private void addExpected(BigDecimal amount){expected=expected.add(amount);}
        private void addCollected(BigDecimal amount){collected=collected.add(amount);}
        private MemberContributionSummary view(){return new MemberContributionSummary(id,name,email,expected,collected,expected.subtract(collected).max(BigDecimal.ZERO),currency);}
    }
}
