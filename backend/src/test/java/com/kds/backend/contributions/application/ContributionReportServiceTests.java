package com.kds.backend.contributions.application;

import com.kds.backend.contributions.domain.ContributionPaymentEntity;
import com.kds.backend.contributions.repository.ContributionPaymentRepository;
import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContributionReportServiceTests {
    private final ContributionScheduleService schedules=mock(ContributionScheduleService.class);
    private final ContributionPaymentRepository payments=mock(ContributionPaymentRepository.class);private final ClubService clubs=mock(ClubService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-03T08:00:00Z"),ZoneOffset.UTC);
    private final ContributionReportService service=new ContributionReportService(schedules,payments,clubs,clock);
    private final UUID clubId=UUID.randomUUID(),actor=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
    @BeforeEach void context(){TenantContext.set(clubId);}
    @AfterEach void clear(){TenantContext.clear();}

    @Test void totalsMatchMemberBreakdownToTheCentAndOverpaymentIsNotDebt(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("REPORTS_READ"));
        when(schedules.expectationsForReport(any(),any())).thenReturn(List.of(expected(first,"Alice","100.10"),expected(second,"Bob","80.20")));
        when(payments.forPeriod(any(),any())).thenReturn(List.of(payment(first,"40.05"),payment(second,"100.20")));
        ContributionReport report=service.summary(actor,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30));
        assertEquals(new BigDecimal("180.30"),report.totalExpected());assertEquals(new BigDecimal("140.25"),report.totalCollected());assertEquals(new BigDecimal("60.05"),report.totalOutstanding());
        assertEquals(new BigDecimal("0.00"),report.members().get(1).outstanding());assertEquals(clock.instant(),report.generatedAt());
    }
    @Test void missingReportPermissionStopsBeforeFinancialQueries(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club());
        assertThrows(AccessDeniedException.class,()->service.summary(actor,LocalDate.now(clock),LocalDate.now(clock)));
        verifyNoInteractions(schedules,payments);
    }
    private ExpectedContribution expected(UUID member,String name,String amount){return new ExpectedContribution(UUID.randomUUID(),UUID.randomUUID(),"Monthly",member,name.toLowerCase()+"@example.test",name,LocalDate.of(2026,9,1),new BigDecimal(amount),"ZAR");}
    private ContributionPaymentEntity payment(UUID member,String amount){return new ContributionPaymentEntity(UUID.randomUUID(),clubId,UUID.randomUUID(),member,LocalDate.of(2026,9,1),new BigDecimal(amount),"ZAR",LocalDate.of(2026,9,2),null,null,actor,clock.instant());}
    private ClubSummary club(String...permissions){return new ClubSummary(clubId,"Club","INVESTMENT_CLUB",false,List.of(permissions));}
}
