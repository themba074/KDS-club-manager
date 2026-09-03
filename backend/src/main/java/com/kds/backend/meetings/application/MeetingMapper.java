package com.kds.backend.meetings.application;
import com.kds.backend.meetings.domain.*;
import org.mapstruct.*;
import java.time.*;
@Mapper(componentModel="spring")
public interface MeetingMapper {
    @Mapping(target="startsAt",expression="java(toOffset(meeting))") MeetingView view(MeetingEntity meeting);
    MeetingView.AgendaItem agenda(AgendaItemEntity item);
    default OffsetDateTime toOffset(MeetingEntity meeting){return meeting.getStartsAt().atOffset(ZoneOffset.ofTotalSeconds(meeting.getUtcOffsetMinutes()*60));}
}
