package com.kds.backend.meetings.application;
import java.time.OffsetDateTime;
import java.util.List;
public record MeetingCommand(long version,String title,String description,OffsetDateTime startsAt,int durationMinutes,
    String location,String meetingUrl,List<AgendaItem> agendaItems){public record AgendaItem(String title,String description){}}
