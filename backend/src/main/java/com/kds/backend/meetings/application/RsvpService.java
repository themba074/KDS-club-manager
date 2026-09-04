package com.kds.backend.meetings.application;

import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.*;
import com.kds.backend.meetings.domain.MeetingRsvpEntity;
import com.kds.backend.meetings.domain.MeetingRsvpEntity.Response;
import com.kds.backend.meetings.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.UUID;

@Service @Transactional(readOnly=true)
public class RsvpService {
    private final MeetingRepository meetings; private final MeetingParticipationRepository participation; private final ClubService clubs; private final MembershipLifecycleService memberships; private final Clock clock;
    public RsvpService(MeetingRepository meetings,MeetingParticipationRepository participation,ClubService clubs,MembershipLifecycleService memberships,Clock clock){this.meetings=meetings;this.participation=participation;this.clubs=clubs;this.memberships=memberships;this.clock=clock;}
    public RsvpView view(UUID actor,UUID meetingId){var access=require(actor,Permission.MEETINGS_READ);requireMeeting(meetingId);UUID membershipId=memberships.requireCurrentMembership(actor).membershipId();Response own=participation.rsvp(meetingId,membershipId).map(MeetingRsvpEntity::getResponse).orElse(null);return new RsvpView(own,access.permissions().contains(Permission.MEETINGS_WRITE.name())?counts(meetingId):null);}
    @Transactional public RsvpView respond(UUID actor,UUID meetingId,Response response){var access=require(actor,Permission.MEETINGS_READ);var meeting=requireMeeting(meetingId);if(!meeting.getStartsAt().isAfter(clock.instant()))throw new ResponseStatusException(HttpStatus.CONFLICT,"RSVPs are closed for this meeting.");UUID membershipId=memberships.requireCurrentMembership(actor).membershipId();var existing=participation.rsvp(meetingId,membershipId);if(existing.isPresent())existing.get().change(response,clock.instant());else participation.add(new MeetingRsvpEntity(UUID.randomUUID(),TenantContext.requireClubId(),meetingId,membershipId,response,clock.instant()));participation.flush();return new RsvpView(response,access.permissions().contains(Permission.MEETINGS_WRITE.name())?counts(meetingId):null);}
    private RsvpView.Counts counts(UUID meetingId){var values=participation.counts(meetingId);return new RsvpView.Counts(values.getOrDefault(Response.YES,0L),values.getOrDefault(Response.NO,0L),values.getOrDefault(Response.MAYBE,0L));}
    private com.kds.backend.meetings.domain.MeetingEntity requireMeeting(UUID id){return meetings.find(id).orElseThrow(()->new AccessDeniedException("Meeting is unavailable in this club."));}
    private ClubSummary require(UUID actor,Permission permission){var access=clubs.requireMembership(actor,TenantContext.requireClubId());if(!access.permissions().contains(permission.name()))throw new AccessDeniedException("You do not have permission for this action.");return access;}
}
