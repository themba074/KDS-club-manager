package com.kds.backend.meetings.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.MeetingEntity;
import com.kds.backend.meetings.domain.MeetingEntity.AgendaDraft;
import com.kds.backend.meetings.repository.MeetingRepository;
import com.kds.backend.members.application.MemberService;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class MeetingService {
    public enum View { UPCOMING, PAST }
    private static final Logger LOGGER=LoggerFactory.getLogger(MeetingService.class);
    private final MeetingRepository meetings; private final ClubService clubs; private final MemberService members;
    private final MeetingMapper mapper; private final MeetingNotificationPublisher notifications; private final Clock clock;
    public MeetingService(MeetingRepository meetings,ClubService clubs,MemberService members,MeetingMapper mapper,MeetingNotificationPublisher notifications,Clock clock){this.meetings=meetings;this.clubs=clubs;this.members=members;this.mapper=mapper;this.notifications=notifications;this.clock=clock;}
    public List<MeetingView> meetings(UUID actor,View view){require(actor,Permission.MEETINGS_READ);Instant now=clock.instant();return (view==View.UPCOMING?meetings.upcoming(now):meetings.past(now)).stream().map(mapper::view).toList();}
    @Transactional
    public MeetingView create(UUID actor,MeetingCommand command){
        require(actor,Permission.MEETINGS_WRITE);validate(command);
        UUID clubId=TenantContext.requireClubId();Instant now=clock.instant();var meeting=new MeetingEntity(UUID.randomUUID(),clubId,actor,now);
        apply(meeting,command,now);meetings.add(meeting);meetings.flush();notifySafely(MeetingChanged.Type.CREATED,meeting);
        return mapper.view(meeting);
    }
    @Transactional
    public MeetingView edit(UUID actor,UUID meetingId,MeetingCommand command){
        require(actor,Permission.MEETINGS_WRITE);validate(command);
        MeetingEntity meeting=meetings.find(meetingId).orElseThrow(()->new AccessDeniedException("Meeting is unavailable in this club."));
        if(meeting.getStartsAt().isBefore(clock.instant()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Past meetings cannot be edited.");
        if(command.version()!=meeting.getVersion())throw new ResponseStatusException(HttpStatus.CONFLICT,"This meeting changed since you opened it. Reload and try again.");
        apply(meeting,command,clock.instant());meetings.flush();notifySafely(MeetingChanged.Type.UPDATED,meeting);
        return mapper.view(meeting);
    }
    private void apply(MeetingEntity meeting,MeetingCommand command,Instant now){
        meeting.update(command.title().strip(),normalize(command.description()),command.startsAt().toInstant(),command.startsAt().getOffset().getTotalSeconds()/60,
            command.durationMinutes(),normalize(command.location()),normalize(command.meetingUrl()),command.agendaItems().stream().map(item->new AgendaDraft(item.title().strip(),normalize(item.description()))).toList(),now);
    }
    private void validate(MeetingCommand command){
        if(!command.startsAt().toInstant().isAfter(clock.instant()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Meeting time must be in the future.");
        if(blank(command.location())&&blank(command.meetingUrl()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a location or online meeting link.");
        if(command.agendaItems()==null||command.agendaItems().isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Add at least one agenda item.");
    }
    private void notifySafely(MeetingChanged.Type type,MeetingEntity meeting){
        try{notifications.publish(new MeetingChanged(type,meeting.getClubId(),meeting.getId(),meeting.getTitle(),mapper.toOffset(meeting),members.activeMeetingAudience()));}
        catch(RuntimeException failure){LOGGER.warn("Meeting {} saved but notification publication failed",meeting.getId(),failure);}
    }
    private void require(UUID actor,Permission permission){if(!clubs.requireMembership(actor,TenantContext.requireClubId()).permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");}
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static String normalize(String value){return blank(value)?null:value.strip();}
}
