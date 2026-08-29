package com.kds.backend.members.application;

import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MemberIdentityDirectoryService;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.repository.MemberRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kds.backend.members.application.MemberImportReport.RowStatus.INVALID;
import static com.kds.backend.members.application.MemberImportReport.RowStatus.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberImportPreviewServiceTests {
    private final ClubService clubs = mock(ClubService.class);
    private final MemberIdentityDirectoryService identities = mock(MemberIdentityDirectoryService.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final UUID actor = UUID.randomUUID();
    private final UUID clubId = UUID.randomUUID();
    private final MemberImportPreviewService service = new MemberImportPreviewService(
            new MemberCsvParser(), clubs, identities, members,
            Validation.buildDefaultValidatorFactory().getValidator());

    @BeforeEach void setUp() {
        TenantContext.set(clubId);
        when(clubs.requireMembership(actor, clubId)).thenReturn(new ClubSummary(
                clubId, "Club", "INVESTMENT_CLUB", true, List.of("MEMBERS_WRITE")));
    }

    @AfterEach void tearDown() { TenantContext.clear(); }

    @Test void previewKeepsValidRowsAndReportsMixedRowErrors() {
        var file = csv("""
                Email,Given name,Surname,Mobile
                ready@example.test,Ready,Member,0711
                bad-email,Bad,Email,
                duplicate@example.test,First,Duplicate,
                DUPLICATE@example.test,Second,Duplicate,
                existing@example.test,Existing,Member,
                """);
        when(identities.membershipEmails(Set.of("ready@example.test", "bad-email", "duplicate@example.test", "existing@example.test")))
                .thenReturn(Set.of("existing@example.test"));
        when(members.invitationEmails(Set.of("ready@example.test", "bad-email", "duplicate@example.test", "existing@example.test")))
                .thenReturn(Set.of());

        var preview = service.preview(actor, file,
                new MemberImportMapping("Email", "Given name", "Surname", "Mobile"));

        assertEquals(5, preview.totalRows());
        assertEquals(1, preview.readyRows());
        assertEquals(READY, preview.rows().getFirst().status());
        assertEquals(INVALID, preview.rows().get(1).status());
        assertEquals(List.of("Email must be valid."), preview.rows().get(1).errors());
        assertEquals("duplicate@example.test", preview.rows().get(3).email());
        assertEquals("This person is already a club member.", preview.rows().getLast().errors().getFirst());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "members.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
