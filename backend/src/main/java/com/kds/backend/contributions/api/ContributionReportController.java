package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/contribution-reports")
public class ContributionReportController {
    private final ContributionReportService service;
    public ContributionReportController(ContributionReportService service){this.service=service;}
    @GetMapping("/summary") @PreAuthorize("hasAuthority('REPORTS_READ')")
    public ContributionReport summary(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.summary(UUID.fromString(jwt.getSubject()),from,to);}
}
