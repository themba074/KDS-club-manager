package com.kds.backend.documents.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.clubtypeconfig.application.RoleService;
import com.kds.backend.documents.domain.DocumentEntity;
import com.kds.backend.documents.domain.DocumentVersionEntity;
import com.kds.backend.documents.repository.DocumentRepository;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.ClubSummary;
import com.kds.backend.identity.application.MembershipLifecycleService;
import com.kds.backend.identity.application.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly=true)
public class DocumentService {
    static final long MAX_FILE_SIZE=5L*1024*1024;
    private static final Duration DOWNLOAD_VALIDITY=Duration.ofMinutes(5);
    private static final Set<String> ALLOWED_TYPES=Set.of("application/pdf","application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv","text/plain","image/png","image/jpeg");
    private static final Logger LOG=LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository documents; private final ClubService clubs;
    private final MembershipLifecycleService memberships; private final RoleService roles;
    private final FileStorageService storage; private final DocumentMapper mapper; private final Clock clock;

    public DocumentService(DocumentRepository documents,ClubService clubs,MembershipLifecycleService memberships,
                           RoleService roles,FileStorageService storage,DocumentMapper mapper,Clock clock){
        this.documents=documents;this.clubs=clubs;this.memberships=memberships;this.roles=roles;
        this.storage=storage;this.mapper=mapper;this.clock=clock;
    }

    public DocumentLibraryView library(UUID actor){
        ClubSummary club=require(actor,Permission.DOCUMENTS_READ); boolean manager=canManage(club);
        String role=memberships.requireCurrentMembership(actor).roleCode();
        var visible=documents.all().stream().filter(document->manager||document.getVisibleRoleCodes().contains(role)).map(mapper::view).toList();
        var options=manager?roles.roles(club.clubType()).stream()
                .map(item->new DocumentLibraryView.RoleOption(item.code(),item.name())).toList():java.util.List.<DocumentLibraryView.RoleOption>of();
        return new DocumentLibraryView(visible,options,manager);
    }

    @Transactional public DocumentView create(UUID actor,DocumentCommand command,DocumentUpload upload){
        require(actor,Permission.DOCUMENTS_MANAGE);memberships.lockClub();ClubSummary club=require(actor,Permission.DOCUMENTS_MANAGE);
        ValidMetadata metadata=validate(command,club);ValidUpload file=validate(upload);
        UUID documentId=UUID.randomUUID(),versionId=UUID.randomUUID();
        String key=key(documentId,versionId,file.fileName());
        StoredFile stored=storage.storeAt(TenantContext.requireClubId(),key,file.fileName(),file.contentType(),file.content());
        var document=new DocumentEntity(documentId,TenantContext.requireClubId(),actor,metadata.title(),metadata.category(),metadata.roles(),clock.instant());
        document.addVersion(versionId,actor,clock.instant(),stored);documents.add(document);documents.flush();
        audit("document.uploaded",actor,documentId,versionId);return mapper.view(document);
    }

    @Transactional public DocumentView update(UUID actor,UUID id,DocumentCommand command){
        require(actor,Permission.DOCUMENTS_MANAGE);memberships.lockClub();ClubSummary club=require(actor,Permission.DOCUMENTS_MANAGE);
        DocumentEntity document=requireForWrite(id);requireVersion(document,command.version());ValidMetadata metadata=validate(command,club);
        document.updateMetadata(metadata.title(),metadata.category(),metadata.roles(),clock.instant());documents.flush();
        audit("document.metadata_updated",actor,id,null);return mapper.view(document);
    }

    @Transactional public DocumentView replace(UUID actor,UUID id,long version,DocumentUpload upload){
        require(actor,Permission.DOCUMENTS_MANAGE);memberships.lockClub();require(actor,Permission.DOCUMENTS_MANAGE);
        DocumentEntity document=requireForWrite(id);requireVersion(document,version);ValidUpload file=validate(upload);UUID versionId=UUID.randomUUID();
        StoredFile stored=storage.storeAt(TenantContext.requireClubId(),key(id,versionId,file.fileName()),file.fileName(),file.contentType(),file.content());
        document.addVersion(versionId,actor,clock.instant(),stored);documents.flush();audit("document.version_uploaded",actor,id,versionId);
        return mapper.view(document);
    }

    public DocumentDownload download(UUID actor,UUID documentId,UUID versionId){
        ClubSummary club=require(actor,Permission.DOCUMENTS_READ);DocumentEntity document=documents.find(documentId)
                .orElseThrow(()->new AccessDeniedException("Document is unavailable in this club."));
        String role=memberships.requireCurrentMembership(actor).roleCode();
        if(!canManage(club)&&!document.getVisibleRoleCodes().contains(role))throw new AccessDeniedException("Document is not visible to your role.");
        DocumentVersionEntity version=document.requireVersion(versionId);
        if(version==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document version was not found.");
        URI signed=storage.signedDownloadUrl(TenantContext.requireClubId(),version.getStorageKey(),DOWNLOAD_VALIDITY).orElse(null);
        if(signed!=null)return new DocumentDownload(version.getFileName(),version.getContentType(),signed,null);
        StoredContent content=storage.load(TenantContext.requireClubId(),version.getStorageKey());
        return new DocumentDownload(version.getFileName(),version.getContentType(),null,content.content());
    }

    private ValidMetadata validate(DocumentCommand command,ClubSummary club){
        String title=required(command.title(),200,"Enter a document title.");String category=required(command.category(),80,"Enter a category.");
        if(command.visibleRoleCodes()==null||command.visibleRoleCodes().isEmpty())throw bad("Select at least one role that can view this document.");
        Set<String> selected=new LinkedHashSet<>();
        command.visibleRoleCodes().forEach(code->{if(code==null||code.isBlank())throw bad("Select valid visibility roles.");String normalized=code.strip().toUpperCase(Locale.ROOT);roles.requireRole(club.clubType(),normalized);selected.add(normalized);});
        return new ValidMetadata(title,category,Set.copyOf(selected));
    }
    private static ValidUpload validate(DocumentUpload upload){
        if(upload==null||upload.content()==null||upload.content().length==0)throw bad("Choose a non-empty document.");
        String name=required(upload.fileName(),255,"The document needs a file name.");String type=upload.contentType()==null?"":upload.contentType().toLowerCase(Locale.ROOT);
        if(upload.content().length>MAX_FILE_SIZE)throw bad("Documents must be 5 MB or smaller.");
        if(!ALLOWED_TYPES.contains(type))throw bad("This document type is not supported.");
        return new ValidUpload(name,type,upload.content());
    }
    private DocumentEntity requireForWrite(UUID id){return documents.lock(id).orElseThrow(()->new AccessDeniedException("Document is unavailable in this club."));}
    private ClubSummary require(UUID actor,Permission permission){ClubSummary club=clubs.requireMembership(actor,TenantContext.requireClubId());if(!club.permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");return club;}
    private static boolean canManage(ClubSummary club){return club.permissions().contains(Permission.DOCUMENTS_MANAGE.name());}
    private static void requireVersion(DocumentEntity document,long expected){if(document.getVersion()!=expected)throw new ResponseStatusException(HttpStatus.CONFLICT,"This document changed since you opened it. Reload and try again.");}
    private static String key(UUID document,UUID version,String name){String extension="";int dot=name.lastIndexOf('.');if(dot>=0&&dot>=name.length()-6)extension=name.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^.a-z0-9]","");return "documents/"+TenantContext.requireClubId()+"/"+document+"/"+version+extension;}
    private static String required(String value,int max,String message){if(value==null||value.isBlank())throw bad(message);String normalized=value.strip();if(normalized.length()>max)throw bad(message);return normalized;}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static void audit(String event,UUID actor,UUID document,UUID version){LOG.info("audit_event={} actor_id={} club_id={} document_id={} document_version_id={}",event,actor,TenantContext.requireClubId(),document,version);}
    private record ValidMetadata(String title,String category,Set<String> roles){}
    private record ValidUpload(String fileName,String contentType,byte[] content){}
}
