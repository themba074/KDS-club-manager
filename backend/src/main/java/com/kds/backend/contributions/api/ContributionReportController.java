package com.kds.backend.contributions.api;
import com.kds.backend.contributions.application.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/contribution-reports")
public class ContributionReportController {
    private final ContributionReportService service;
    private final ContributionReportExportService exports;
    public ContributionReportController(ContributionReportService service,ContributionReportExportService exports){this.service=service;this.exports=exports;}
    @GetMapping("/summary") @PreAuthorize("hasAuthority('REPORTS_READ')")
    public ContributionReport summary(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.summary(UUID.fromString(jwt.getSubject()),from,to);}
    @GetMapping("/export") @PreAuthorize("hasAuthority('REPORTS_READ')")
    public ResponseEntity<StreamingResponseBody> export(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam ContributionReportFormat format){
        ContributionReport report=service.summary(UUID.fromString(jwt.getSubject()),from,to);ContributionReportExporter exporter=exports.require(format);
        String filename="contributions-"+from+"-to-"+to+"."+exporter.extension();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(filename).build().toString())
            .contentType(MediaType.parseMediaType(exporter.mediaType())).body(output->exporter.write(report,output));
    }
}
