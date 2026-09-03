package com.kds.backend.meetings.application;
import com.kds.backend.members.application.MeetingAudienceMember;
import java.time.OffsetDateTime;
import java.util.*;
public record MeetingChanged(Type type,UUID clubId,UUID meetingId,String title,OffsetDateTime startsAt,List<MeetingAudienceMember> recipients){public enum Type{CREATED,UPDATED}}
