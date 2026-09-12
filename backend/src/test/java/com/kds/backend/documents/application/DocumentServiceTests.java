package com.kds.backend.documents.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.clubtypeconfig.application.RoleDefinition;
import com.kds.backend.clubtypeconfig.application.RoleService;
import com.kds.backend.documents.domain.DocumentEntity;
import com.kds.backend.documents.repository.DocumentRepository;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleMember;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTests {
    private final DocumentRepository documents=mock(DocumentRepository.class);private final ClubService clubs=mock(ClubService.class);
    private final MembershipLifecycleService memberships=mock(MembershipLifecycleService.class);private final RoleService roles=mock(RoleService.class);
    private final FileStorageService storage=mock(FileStorageService.class);private final UUID club=UUID.randomUUID(),actor=UUID.randomUUID(),membership=UUID.randomUUID();
    private final Instant now=Instant.parse("2026-09-12T10:00:00Z");private DocumentService service;
    @BeforeEach void setup(){TenantContext.set(club);service=new DocumentService(documents,clubs,memberships,roles,storage,new DocumentMapperImpl(),Clock.fixed(now,ZoneOffset.UTC));
        when(clubs.requireMembership(actor,club)).thenReturn(summary("DOCUMENTS_READ","DOCUMENTS_MANAGE"));when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(membership,actor,"MEMBER","ACTIVE"));
        when(roles.requireRole(eq("INVESTMENT_CLUB"),anyString())).thenAnswer(call->new RoleDefinition(call.getArgument(1),call.getArgument(1),Set.of(Permission.DOCUMENTS_READ)));
        when(roles.roles("INVESTMENT_CLUB")).thenReturn(List.of(new RoleDefinition("MEMBER","Member",Set.of(Permission.DOCUMENTS_READ))));
        when(storage.storeAt(eq(club),anyString(),anyString(),anyString(),any())).thenAnswer(call->new StoredFile(call.getArgument(1),call.getArgument(2),call.getArgument(3),((byte[])call.getArgument(4)).length));}
    @AfterEach void clear(){TenantContext.clear();}

    @Test void createUsesTenantDocumentVersionKeyAndChecksPermissionUnderLock(){var view=service.create(actor,command(0),file("rules.pdf","application/pdf",4));assertEquals("Rules",view.title());assertEquals(1,view.versions().size());
        var key=org.mockito.ArgumentCaptor.forClass(String.class);verify(storage).storeAt(eq(club),key.capture(),eq("rules.pdf"),eq("application/pdf"),any());assertTrue(key.getValue().matches("documents/"+club+"/[0-9a-f-]+/[0-9a-f-]+\\.pdf"));
        var order=inOrder(clubs,memberships);order.verify(clubs).requireMembership(actor,club);order.verify(memberships).lockClub();order.verify(clubs).requireMembership(actor,club);}

    @Test void ordinaryMemberOnlySeesDocumentsForCurrentRole(){DocumentEntity visible=document(Set.of("MEMBER"));DocumentEntity hidden=document(Set.of("TREASURER"));when(documents.all()).thenReturn(List.of(visible,hidden));when(clubs.requireMembership(actor,club)).thenReturn(summary("DOCUMENTS_READ"));
        var library=service.library(actor);assertEquals(1,library.documents().size());assertFalse(library.canManage());assertTrue(library.visibilityRoles().isEmpty());}

    @Test void directDownloadRepeatsRoleCheckAndSupportsSignedLinks(){DocumentEntity hidden=document(Set.of("TREASURER"));when(documents.find(hidden.getId())).thenReturn(Optional.of(hidden));when(clubs.requireMembership(actor,club)).thenReturn(summary("DOCUMENTS_READ"));
        UUID version=hidden.getVersions().getFirst().getId();assertThrows(AccessDeniedException.class,()->service.download(actor,hidden.getId(),version));
        when(memberships.requireCurrentMembership(actor)).thenReturn(new MembershipLifecycleMember(membership,actor,"TREASURER","ACTIVE"));when(storage.signedDownloadUrl(eq(club),anyString(),any())).thenReturn(Optional.of(URI.create("https://example.test/signed")));
        assertEquals("https://example.test/signed",service.download(actor,hidden.getId(),version).signedUrl().toString());verify(storage,never()).load(any(),any());}

    @Test void staleWritesInvalidRolesAndUnsafeFilesAreRejected(){DocumentEntity existing=document(Set.of("MEMBER"));when(documents.lock(existing.getId())).thenReturn(Optional.of(existing));assertStatus(409,()->service.update(actor,existing.getId(),new DocumentCommand(7,"Changed","Policy",Set.of("MEMBER"))));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Unknown role")).when(roles).requireRole("INVESTMENT_CLUB","UNKNOWN");assertStatus(400,()->service.create(actor,new DocumentCommand(0,"Rules","Policy",Set.of("UNKNOWN")),file("rules.pdf","application/pdf",4)));
        assertStatus(400,()->service.create(actor,command(0),file("script.html","text/html",4)));assertStatus(400,()->service.create(actor,command(0),file("huge.pdf","application/pdf",(int)DocumentService.MAX_FILE_SIZE+1)));}

    private DocumentEntity document(Set<String> visible){DocumentEntity item=new DocumentEntity(UUID.randomUUID(),club,actor,"Rules","Policy",visible,now);UUID version=UUID.randomUUID();item.addVersion(version,actor,now,new StoredFile("documents/"+club+"/d/"+version+".pdf","rules.pdf","application/pdf",4));return item;}
    private DocumentCommand command(long version){return new DocumentCommand(version," Rules "," Policy ",Set.of("MEMBER"));}
    private DocumentUpload file(String name,String type,int size){return new DocumentUpload(name,type,new byte[size]);}
    private ClubSummary summary(String... permissions){return new ClubSummary(club,"Club","INVESTMENT_CLUB",true,List.of(permissions));}
    private static void assertStatus(int expected,org.junit.jupiter.api.function.Executable action){assertEquals(expected,assertThrows(ResponseStatusException.class,action).getStatusCode().value());}
}
