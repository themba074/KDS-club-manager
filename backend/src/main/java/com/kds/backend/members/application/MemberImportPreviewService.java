package com.kds.backend.members.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.MemberIdentityDirectoryService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.repository.MemberRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kds.backend.members.application.MemberImportReport.RowStatus.INVALID;
import static com.kds.backend.members.application.MemberImportReport.RowStatus.READY;

@Service
@Transactional(readOnly = true)
public class MemberImportPreviewService {
    private final MemberCsvParser parser;
    private final ClubService clubs;
    private final MemberIdentityDirectoryService identityDirectory;
    private final MemberRepository members;
    private final Validator validator;

    public MemberImportPreviewService(MemberCsvParser parser, ClubService clubs,
                                      MemberIdentityDirectoryService identityDirectory,
                                      MemberRepository members, Validator validator) {
        this.parser = parser;
        this.clubs = clubs;
        this.identityDirectory = identityDirectory;
        this.members = members;
        this.validator = validator;
    }

    public MemberImportReport.Preview preview(UUID actor, MultipartFile file, MemberImportMapping mapping) {
        requireWriteAccess(actor);
        MemberCsvParser.ParsedCsv csv = parser.parse(file);
        validateMapping(csv.headers(), mapping);

        List<CandidateRow> candidates = csv.rows().stream().map(row -> candidate(row, mapping)).toList();
        Map<String, Long> occurrences = candidates.stream()
                .filter(row -> !row.email().isBlank())
                .collect(Collectors.groupingBy(CandidateRow::email, HashMap::new, Collectors.counting()));
        Set<String> emails = candidates.stream().map(CandidateRow::email)
                .filter(email -> !email.isBlank()).collect(Collectors.toSet());
        Set<String> existingMembers = identityDirectory.membershipEmails(emails);
        Set<String> existingInvitations = members.invitationEmails(emails);

        List<MemberImportReport.Row> rows = candidates.stream().map(candidate -> {
            List<String> errors = validationErrors(candidate);
            if (occurrences.getOrDefault(candidate.email(), 0L) > 1) errors.add("Email appears more than once in this file.");
            if (existingMembers.contains(candidate.email())) errors.add("This person is already a club member.");
            if (existingInvitations.contains(candidate.email())) errors.add("An invitation already exists for this email address.");
            return new MemberImportReport.Row(candidate.rowNumber(), candidate.email(), candidate.firstName(),
                    candidate.lastName(), candidate.phone(), errors.isEmpty() ? READY : INVALID, List.copyOf(errors));
        }).toList();
        int ready = Math.toIntExact(rows.stream().filter(row -> row.status() == READY).count());
        return new MemberImportReport.Preview(rows.size(), ready, rows.size() - ready, rows);
    }

    private CandidateRow candidate(MemberCsvParser.ParsedRow row, MemberImportMapping mapping) {
        String email = row.values().get(mapping.emailColumn()).strip().toLowerCase(Locale.ROOT);
        String firstName = row.values().get(mapping.firstNameColumn()).strip();
        String lastName = row.values().get(mapping.lastNameColumn()).strip();
        String phone = mapping.phoneColumn() == null || mapping.phoneColumn().isBlank()
                ? null : blankToNull(row.values().get(mapping.phoneColumn()));
        return new CandidateRow(row.rowNumber(), email, firstName, lastName, phone);
    }

    private List<String> validationErrors(CandidateRow candidate) {
        return validator.validate(candidate).stream()
                .sorted((left, right) -> left.getPropertyPath().toString().compareTo(right.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validateMapping(List<String> headers, MemberImportMapping mapping) {
        if (mapping == null) throw badRequest("Choose columns for email, first name, and last name.");
        List<String> requiredColumns = List.of(
                mapping.emailColumn() == null ? "" : mapping.emailColumn(),
                mapping.firstNameColumn() == null ? "" : mapping.firstNameColumn(),
                mapping.lastNameColumn() == null ? "" : mapping.lastNameColumn());
        if (requiredColumns.stream().anyMatch(String::isBlank)) {
            throw badRequest("Choose columns for email, first name, and last name.");
        }
        Set<String> required = new LinkedHashSet<>(requiredColumns);
        if (required.size() != 3) throw badRequest("Email, first name, and last name must use different columns.");
        Set<String> selected = new HashSet<>(required);
        if (mapping.phoneColumn() != null && !mapping.phoneColumn().isBlank() && !selected.add(mapping.phoneColumn())) {
            throw badRequest("Phone must use a different column from the required fields.");
        }
        if (!headers.containsAll(selected)) throw badRequest("One or more mapped columns are not present in the CSV file.");
    }

    public void requireWriteAccess(UUID actor) {
        var club = clubs.requireMembership(actor, TenantContext.requireClubId());
        if (!club.permissions().contains(Permission.MEMBERS_WRITE.name())) {
            throw new AccessDeniedException("You do not have permission for this action.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record CandidateRow(
            int rowNumber,
            @NotBlank(message = "Email is required.") @Email(message = "Email must be valid.")
            @Size(max = 320, message = "Email must be 320 characters or fewer.") String email,
            @NotBlank(message = "First name is required.")
            @Size(max = 80, message = "First name must be 80 characters or fewer.") String firstName,
            @NotBlank(message = "Last name is required.")
            @Size(max = 80, message = "Last name must be 80 characters or fewer.") String lastName,
            @Size(max = 30, message = "Phone must be 30 characters or fewer.") String phone) {}
}
