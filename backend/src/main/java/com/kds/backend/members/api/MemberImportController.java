package com.kds.backend.members.api;

import com.kds.backend.members.application.MemberImportReport;
import com.kds.backend.members.application.MemberImportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/member-imports")
public class MemberImportController {
    private final MemberImportService imports;

    public MemberImportController(MemberImportService imports) {
        this.imports = imports;
    }

    @PostMapping("/inspect")
    @PreAuthorize("hasAuthority('MEMBERS_WRITE')")
    public MemberImportReport.Inspection inspect(@AuthenticationPrincipal Jwt principal,
                                                 @RequestParam MultipartFile file) {
        return imports.inspect(UUID.fromString(principal.getSubject()), file);
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('MEMBERS_WRITE')")
    public MemberImportReport.Preview preview(@AuthenticationPrincipal Jwt principal,
                                             @RequestParam MultipartFile file,
                                             @Valid @ModelAttribute MemberImportDtos.ColumnMapping mapping) {
        return imports.preview(UUID.fromString(principal.getSubject()), file, mapping.toApplication());
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('MEMBERS_WRITE')")
    public MemberImportReport.Confirmation confirm(@AuthenticationPrincipal Jwt principal,
                                                   @RequestParam MultipartFile file,
                                                   @Valid @ModelAttribute MemberImportDtos.ColumnMapping mapping) {
        return imports.confirm(UUID.fromString(principal.getSubject()), file, mapping.toApplication());
    }
}
