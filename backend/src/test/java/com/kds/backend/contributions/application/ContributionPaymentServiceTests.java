package com.kds.backend.contributions.application;

import com.kds.backend.contributions.domain.ContributionPaymentEntity;
import com.kds.backend.contributions.repository.ContributionPaymentRepository;
import com.kds.backend.documents.application.*;
import com.kds.backend.identity.application.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContributionPaymentServiceTests {
    private final ContributionPaymentRepository payments=mock(ContributionPaymentRepository.class);
    private final ContributionScheduleService schedules=mock(ContributionScheduleService.class);
    private final ClubService clubs=mock(ClubService.class); private final MembershipLifecycleService memberships=mock(MembershipLifecycleService.class);
    private final FileStorageService storage=mock(FileStorageService.class); private final Clock clock=Clock.fixed(Instant.parse("2026-09-02T10:00:00Z"),ZoneOffset.UTC);
    private final ContributionPaymentService service=new ContributionPaymentService(payments,schedules,clubs,memberships,storage,clock);
    private final UUID clubId=UUID.randomUUID(),actor=UUID.randomUUID(),memberId=UUID.randomUUID(),versionId=UUID.randomUUID();
    @BeforeEach void context(){TenantContext.set(clubId);}
    @AfterEach void clear(){TenantContext.clear();}

    @Test void ledgerCombinesExpectedAndPartialPaymentsInChronologicalBalance(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("CONTRIBUTIONS_READ"));
        when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(memberId,actor,"MEMBER","ACTIVE"));
        when(schedules.expectationsForMembership(any(),any(),eq(memberId))).thenReturn(List.of(expected(new BigDecimal("100.00"))));
        when(payments.forMembership(eq(memberId),any(),any())).thenReturn(List.of(payment("40.00",LocalDate.of(2026,9,2)),payment("10.00",LocalDate.of(2026,9,3))));
        MemberLedger ledger=service.myLedger(actor,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30));
        assertEquals(new BigDecimal("100.00"),ledger.totalExpected());assertEquals(new BigDecimal("50.00"),ledger.totalPaid());assertEquals(new BigDecimal("50.00"),ledger.balance());
        assertEquals(List.of(new BigDecimal("100.00"),new BigDecimal("60.00"),new BigDecimal("50.00")),ledger.lines().stream().map(LedgerLine::runningBalance).toList());
    }
    @Test void recordValidatesExpectationAndStoresOptionalProof(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("CONTRIBUTIONS_WRITE"));when(schedules.requireExpectation(versionId,memberId,LocalDate.of(2026,9,1))).thenReturn(expected(new BigDecimal("100.00")));
        when(storage.store(eq(clubId),eq("payment-proofs"),eq("proof.pdf"),eq("application/pdf"),any())).thenReturn(new StoredFile("key","proof.pdf","application/pdf",3));
        PaymentView result=service.record(actor,new PaymentCommand(versionId,memberId,LocalDate.of(2026,9,1),new BigDecimal("40.00"),LocalDate.of(2026,9,2),"EFT-1",null),new PaymentProof("proof.pdf","application/pdf",new byte[]{1,2,3}));
        assertEquals(new BigDecimal("40.00"),result.amount());assertEquals("proof.pdf",result.proofFileName());verify(payments).add(any(ContributionPaymentEntity.class));
    }
    @Test void futurePaymentAndUnsafeProofAreRejected(){
        when(clubs.requireMembership(actor,clubId)).thenReturn(club("CONTRIBUTIONS_WRITE"));
        var future=new PaymentCommand(versionId,memberId,LocalDate.of(2026,9,1),BigDecimal.TEN,LocalDate.of(2026,9,3),null,null);
        assertEquals(400,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.record(actor,future,null)).getStatusCode().value());
        var valid=new PaymentCommand(versionId,memberId,LocalDate.of(2026,9,1),BigDecimal.TEN,LocalDate.of(2026,9,2),null,null);
        assertEquals(400,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.record(actor,valid,new PaymentProof("proof.exe","application/octet-stream",new byte[]{1}))).getStatusCode().value());
        verifyNoInteractions(schedules,payments,storage);
    }
    private ExpectedContribution expected(BigDecimal amount){return new ExpectedContribution(UUID.randomUUID(),versionId,"Monthly",memberId,"member@example.test","Member",LocalDate.of(2026,9,1),amount,"ZAR");}
    private ContributionPaymentEntity payment(String amount,LocalDate received){return new ContributionPaymentEntity(UUID.randomUUID(),clubId,versionId,memberId,LocalDate.of(2026,9,1),new BigDecimal(amount),"ZAR",received,null,null,actor,clock.instant());}
    private ClubSummary club(String...permissions){return new ClubSummary(clubId,"Club","INVESTMENT_CLUB",false,List.of(permissions));}
}
