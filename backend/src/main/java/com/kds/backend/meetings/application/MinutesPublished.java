package com.kds.backend.meetings.application;

import com.kds.backend.members.application.MeetingAudienceMember;
import java.util.*;
public record MinutesPublished(UUID clubId,UUID meetingId,String meetingTitle,List<MeetingAudienceMember> recipients) {}
