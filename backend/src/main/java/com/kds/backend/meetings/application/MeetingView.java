package com.kds.backend.meetings.application;
import java.time.OffsetDateTime;
import java.util.*;
public record MeetingView(UUID id,long version,String title,String description,OffsetDateTime startsAt,int durationMinutes,
    String location,String meetingUrl,List<AgendaItem> agendaItems){public record AgendaItem(UUID id,int position,String title,String description){}}
