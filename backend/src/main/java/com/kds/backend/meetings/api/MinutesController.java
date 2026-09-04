package com.kds.backend.meetings.api;

import com.kds.backend.meetings.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/meetings/{meetingId}/minutes")
public class MinutesController {
    private final MinutesService service;
    public MinutesController(MinutesService service){this.service=service;}
    public record SaveRequest(@PositiveOrZero long version,@Size(max=20000) String body) {}
    @GetMapping @PreAuthorize("hasAuthority('MEETINGS_READ')") public MinutesView view(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId){return service.view(actor(jwt),meetingId);}
    @PutMapping @PreAuthorize("hasAuthority('MEETINGS_WRITE')") public MinutesView save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId,@Valid @RequestBody SaveRequest request){return service.save(actor(jwt),meetingId,request.version(),request.body());}
    @PostMapping(value="/attachment",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('MEETINGS_WRITE')") public MinutesView attach(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId,@RequestParam @PositiveOrZero long version,@RequestPart("file") MultipartFile file) throws IOException{return service.attach(actor(jwt),meetingId,version,new MinutesAttachment(file.getOriginalFilename(),file.getContentType(),file.getBytes()));}
    @PostMapping("/publish") @PreAuthorize("hasAuthority('MEETINGS_WRITE')") public MinutesView publish(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId,@Valid @RequestBody PublishRequest request){return service.publish(actor(jwt),meetingId,request.version());}
    public record PublishRequest(@PositiveOrZero long version) {}
    @GetMapping("/attachment") @PreAuthorize("hasAuthority('MEETINGS_READ')") public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID meetingId){var file=service.download(actor(jwt),meetingId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.fileName()).build().toString()).body(file.content());}
    private static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
