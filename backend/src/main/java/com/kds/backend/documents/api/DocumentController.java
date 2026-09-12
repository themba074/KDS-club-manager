package com.kds.backend.documents.api;

import com.kds.backend.documents.application.DocumentCommand;
import com.kds.backend.documents.application.DocumentLibraryView;
import com.kds.backend.documents.application.DocumentService;
import com.kds.backend.documents.application.DocumentUpload;
import com.kds.backend.documents.application.DocumentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service){this.service=service;}

    public record Metadata(@PositiveOrZero long version,@NotBlank @Size(max=200) String title,
                           @NotBlank @Size(max=80) String category,
                           @NotEmpty Set<@NotBlank @Size(max=80) String> visibleRoleCodes){
        DocumentCommand command(){return new DocumentCommand(version,title,category,visibleRoleCodes);}
    }

    @GetMapping @PreAuthorize("hasAuthority('DOCUMENTS_READ')")
    public DocumentLibraryView library(@AuthenticationPrincipal Jwt jwt){return service.library(actor(jwt));}

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('DOCUMENTS_MANAGE')")
    public DocumentView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestPart("metadata") Metadata metadata,
                               @RequestPart("file") MultipartFile file)throws IOException{
        return service.create(actor(jwt),metadata.command(),upload(file));
    }

    @PutMapping("/{documentId}") @PreAuthorize("hasAuthority('DOCUMENTS_MANAGE')")
    public DocumentView update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID documentId,
                               @Valid @RequestBody Metadata metadata){return service.update(actor(jwt),documentId,metadata.command());}

    @PostMapping(value="/{documentId}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENTS_MANAGE')")
    public DocumentView replace(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID documentId,
                                @RequestParam @PositiveOrZero long version,@RequestPart("file") MultipartFile file)throws IOException{
        return service.replace(actor(jwt),documentId,version,upload(file));
    }

    @GetMapping("/{documentId}/versions/{versionId}/download") @PreAuthorize("hasAuthority('DOCUMENTS_READ')")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID documentId,@PathVariable UUID versionId){
        var download=service.download(actor(jwt),documentId,versionId);
        if(download.signedUrl()!=null)return ResponseEntity.status(HttpStatus.FOUND).location(download.signedUrl()).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .body(download.content());
    }
    private static DocumentUpload upload(MultipartFile file)throws IOException{return new DocumentUpload(file.getOriginalFilename(),file.getContentType(),file.getBytes());}
    private static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
