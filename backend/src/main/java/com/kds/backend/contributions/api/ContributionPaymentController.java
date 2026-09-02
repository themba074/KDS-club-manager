package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/contribution-payments")
public class ContributionPaymentController {
    private final ContributionPaymentService service;
    public ContributionPaymentController(ContributionPaymentService service){this.service=service;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')")
    public PaymentView record(@AuthenticationPrincipal Jwt jwt,@Valid @RequestPart("payment") ContributionPaymentDtos.PaymentRequest request,
            @RequestPart(value="proof",required=false) MultipartFile proof) throws IOException {
        PaymentProof value=proof==null?null:new PaymentProof(proof.getOriginalFilename(),proof.getContentType(),proof.getBytes());
        return service.record(actor(jwt),request.command(),value);
    }
    @GetMapping("/expectations") @PreAuthorize("hasAuthority('CONTRIBUTIONS_WRITE')")
    public List<ContributionExpectationStatus> expectations(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.recordableExpectations(actor(jwt),from,to);}
    @GetMapping("/my-ledger") @PreAuthorize("hasAuthority('CONTRIBUTIONS_READ')")
    public MemberLedger ledger(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.myLedger(actor(jwt),from,to);}
    private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
