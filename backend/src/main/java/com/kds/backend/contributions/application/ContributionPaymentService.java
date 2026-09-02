package com.kds.backend.contributions.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.contributions.domain.ContributionPaymentEntity;
import com.kds.backend.contributions.repository.ContributionPaymentRepository;
import com.kds.backend.documents.application.FileStorageService;
import com.kds.backend.identity.application.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ContributionPaymentService {
    private static final Set<String> PROOF_TYPES=Set.of("application/pdf","image/jpeg","image/png");
    private static final int MAX_PROOF_BYTES=1024*1024;
    private final ContributionPaymentRepository payments; private final ContributionScheduleService schedules;
    private final ClubService clubs; private final MembershipLifecycleService memberships; private final FileStorageService storage; private final Clock clock;
    public ContributionPaymentService(ContributionPaymentRepository payments,ContributionScheduleService schedules,ClubService clubs,
            MembershipLifecycleService memberships,FileStorageService storage,Clock clock){
        this.payments=payments;this.schedules=schedules;this.clubs=clubs;this.memberships=memberships;this.storage=storage;this.clock=clock;
    }
    @Transactional
    public PaymentView record(UUID actor,PaymentCommand command,PaymentProof proof){
        require(actor,Permission.CONTRIBUTIONS_WRITE); validate(command,proof);
        ExpectedContribution expectation=schedules.requireExpectation(command.scheduleVersionId(),command.membershipId(),command.dueDate());
        UUID clubId=TenantContext.requireClubId(); Instant now=clock.instant();
        var payment=new ContributionPaymentEntity(UUID.randomUUID(),clubId,expectation.scheduleVersionId(),expectation.membershipId(),
            expectation.dueDate(),command.amount(),expectation.currency(),command.receivedOn(),normalize(command.reference()),normalize(command.note()),actor,now);
        if(proof!=null){var stored=storage.store(clubId,"payment-proofs",proof.fileName(),proof.contentType(),proof.content());payment.attachProof(stored.storageKey(),stored.fileName(),stored.contentType());}
        payments.add(payment); return view(payment,expectation);
    }
    public List<ContributionExpectationStatus> recordableExpectations(UUID actor,LocalDate from,LocalDate to){
        require(actor,Permission.CONTRIBUTIONS_WRITE);
        return schedules.upcoming(actor,from,to).stream().map(expected->{
            BigDecimal paid=payments.forExpectation(expected.scheduleVersionId(),expected.membershipId(),expected.dueDate()).stream()
                .map(ContributionPaymentEntity::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            return new ContributionExpectationStatus(expected.scheduleVersionId(),expected.scheduleName(),expected.membershipId(),expected.memberName(),
                expected.dueDate(),expected.amount(),paid,expected.amount().subtract(paid),expected.currency());
        }).toList();
    }
    public MemberLedger myLedger(UUID actor,LocalDate from,LocalDate to){
        require(actor,Permission.CONTRIBUTIONS_READ); validateRange(from,to);
        UUID membershipId=memberships.requireCurrentMembership(actor).membershipId();
        var expected=schedules.expectationsForMembership(from,to,membershipId); var actual=payments.forMembership(membershipId,from,to);
        List<Event> events=new ArrayList<>();
        expected.forEach(item->events.add(new Event(item.dueDate(),0,"EXPECTED",item.scheduleName(),item.scheduleVersionId(),null,item.amount(),BigDecimal.ZERO,item.currency())));
        actual.forEach(item->events.add(new Event(item.getReceivedOn(),1,"PAYMENT",paymentDescription(item),item.getScheduleVersionId(),item.getId(),BigDecimal.ZERO,item.getAmount(),item.getCurrency())));
        events.sort(Comparator.comparing(Event::date).thenComparingInt(Event::order).thenComparing(Event::description));
        BigDecimal balance=BigDecimal.ZERO; List<LedgerLine> lines=new ArrayList<>();
        for(Event event:events){balance=balance.add(event.expected()).subtract(event.paid());lines.add(new LedgerLine(event.type(),event.date(),event.description(),event.version(),event.payment(),event.expected(),event.paid(),balance,event.currency()));}
        BigDecimal totalExpected=expected.stream().map(ExpectedContribution::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal totalPaid=actual.stream().map(ContributionPaymentEntity::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        String currency=expected.stream().map(ExpectedContribution::currency).findFirst().orElseGet(()->actual.stream().map(ContributionPaymentEntity::getCurrency).findFirst().orElse("ZAR"));
        return new MemberLedger(membershipId,from,to,totalExpected,totalPaid,totalExpected.subtract(totalPaid),currency,List.copyOf(lines));
    }
    private void validate(PaymentCommand command,PaymentProof proof){
        if(command.receivedOn().isAfter(LocalDate.now(clock)))throw bad("Payment date cannot be in the future.");
        if(proof!=null&&(proof.content()==null||proof.content().length==0||proof.content().length>MAX_PROOF_BYTES))throw bad("Proof must be a non-empty file no larger than 1 MB.");
        if(proof!=null&&!PROOF_TYPES.contains(proof.contentType()))throw bad("Proof must be a PDF, JPEG, or PNG file.");
    }
    private void validateRange(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||to.isAfter(from.plusYears(1)))throw bad("Choose a valid date range of at most one year.");}
    private void require(UUID actor,Permission permission){if(!clubs.requireMembership(actor,TenantContext.requireClubId()).permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");}
    private PaymentView view(ContributionPaymentEntity p,ExpectedContribution e){return new PaymentView(p.getId(),p.getScheduleVersionId(),e.scheduleName(),p.getMembershipId(),e.memberName(),p.getDueDate(),p.getAmount(),p.getCurrency(),p.getReceivedOn(),p.getReference(),p.getNote(),p.getProofFileName(),p.getCreatedAt());}
    private static String paymentDescription(ContributionPaymentEntity payment){return payment.getReference()==null?"Payment received":"Payment received · "+payment.getReference();}
    private static String normalize(String value){return value==null||value.isBlank()?null:value.strip();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private record Event(LocalDate date,int order,String type,String description,UUID version,UUID payment,BigDecimal expected,BigDecimal paid,String currency){}
}
