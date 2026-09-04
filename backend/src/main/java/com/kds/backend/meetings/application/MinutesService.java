package com.kds.backend.meetings.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.documents.application.*;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.MeetingMinutesEntity;
import com.kds.backend.meetings.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.*;

@Service @Transactional(readOnly=true)
public class MinutesService {
    private static final int MAX_FILE_BYTES=5*1024*1024;
    private static final Set<String> FILE_TYPES=Set.of("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain");
    private final MeetingRepository meetings; private final MeetingParticipationRepository participation; private final ClubService clubs; private final FileStorageService storage; private final Clock clock;
    public MinutesService(MeetingRepository meetings,MeetingParticipationRepository participation,ClubService clubs,FileStorageService storage,Clock clock){this.meetings=meetings;this.participation=participation;this.clubs=clubs;this.storage=storage;this.clock=clock;}
    public MinutesView view(UUID actor,UUID meetingId){var access=require(actor,Permission.MEETINGS_READ);requireMeeting(meetingId);var minutes=requireMinutes(meetingId);if(minutes.getPublishedAt()==null&&!access.permissions().contains(Permission.MEETINGS_WRITE.name()))throw new AccessDeniedException("Minutes have not been published.");return view(minutes);}
    @Transactional public MinutesView save(UUID actor,UUID meetingId,long version,String body){require(actor,Permission.MEETINGS_WRITE);requirePastMeeting(meetingId);String normalized=normalize(body);var existing=participation.minutes(meetingId);boolean created=existing.isEmpty();MeetingMinutesEntity minutes;if(created){if(normalized==null)throw bad("Add formatted minutes or an attachment.");minutes=new MeetingMinutesEntity(UUID.randomUUID(),TenantContext.requireClubId(),meetingId,actor,clock.instant());}else{minutes=existing.get();requireVersion(minutes,version);if(normalized==null&&minutes.getAttachmentKey()==null)throw bad("Add formatted minutes or an attachment.");}minutes.updateBody(normalized,clock.instant());if(created)participation.add(minutes);participation.flush();return view(minutes);}
    @Transactional public MinutesView attach(UUID actor,UUID meetingId,long version,MinutesAttachment attachment){require(actor,Permission.MEETINGS_WRITE);requirePastMeeting(meetingId);validate(attachment);var existing=participation.minutes(meetingId);boolean created=existing.isEmpty();MeetingMinutesEntity minutes;if(created){minutes=new MeetingMinutesEntity(UUID.randomUUID(),TenantContext.requireClubId(),meetingId,actor,clock.instant());}else{minutes=existing.get();requireVersion(minutes,version);}var stored=storage.store(TenantContext.requireClubId(),"meeting-minutes",attachment.fileName(),attachment.contentType(),attachment.content());minutes.attach(stored,clock.instant());if(created)participation.add(minutes);participation.flush();return view(minutes);}
    @Transactional public MinutesView publish(UUID actor,UUID meetingId,long version){require(actor,Permission.MEETINGS_WRITE);requirePastMeeting(meetingId);var minutes=requireMinutes(meetingId);requireVersion(minutes,version);if(minutes.getBody()==null&&minutes.getAttachmentKey()==null)throw bad("Add minutes before publishing.");minutes.publish(clock.instant());participation.flush();return view(minutes);}
    public Download download(UUID actor,UUID meetingId){var access=require(actor,Permission.MEETINGS_READ);requireMeeting(meetingId);var minutes=requireMinutes(meetingId);if(minutes.getPublishedAt()==null&&!access.permissions().contains(Permission.MEETINGS_WRITE.name()))throw new AccessDeniedException("Minutes have not been published.");if(minutes.getAttachmentKey()==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No minutes attachment is available.");var content=storage.load(TenantContext.requireClubId(),minutes.getAttachmentKey());return new Download(minutes.getAttachmentName(),minutes.getAttachmentContentType(),content.content());}
    public record Download(String fileName,String contentType,byte[] content) {}
    private void validate(MinutesAttachment file){if(file==null||file.content()==null||file.content().length==0||file.content().length>MAX_FILE_BYTES)throw bad("Minutes attachment must be a non-empty file no larger than 5 MB.");if(!FILE_TYPES.contains(file.contentType()))throw bad("Minutes attachment must be a PDF, DOCX, or text file.");}
    private void requireVersion(MeetingMinutesEntity minutes,long version){if(minutes.getVersion()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"These minutes changed since you opened them. Reload and try again.");}
    private void requirePastMeeting(UUID id){if(requireMeeting(id).getStartsAt().isAfter(clock.instant()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Minutes can be captured after the meeting starts.");}
    private com.kds.backend.meetings.domain.MeetingEntity requireMeeting(UUID id){return meetings.find(id).orElseThrow(()->new AccessDeniedException("Meeting is unavailable in this club."));}
    private MeetingMinutesEntity requireMinutes(UUID id){return participation.minutes(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Minutes are not available."));}
    private ClubSummary require(UUID actor,Permission permission){var access=clubs.requireMembership(actor,TenantContext.requireClubId());if(!access.permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");return access;}
    private static MinutesView view(MeetingMinutesEntity value){return new MinutesView(value.getId(),value.getVersion(),value.getBody(),value.getAttachmentName(),value.getAttachmentSize(),value.getPublishedAt(),value.getPublishedAt()!=null);}
    private static String normalize(String value){return value==null||value.isBlank()?null:value.strip();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
