package com.kds.backend.meetings.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="meeting_rsvps")
public class MeetingRsvpEntity {
    @Id private UUID id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(name="meeting_id",nullable=false) private UUID meetingId;
    @Column(name="membership_id",nullable=false) private UUID membershipId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Response response;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected MeetingRsvpEntity() {}
    public MeetingRsvpEntity(UUID id,UUID clubId,UUID meetingId,UUID membershipId,Response response,Instant now){this.id=id;this.clubId=clubId;this.meetingId=meetingId;this.membershipId=membershipId;this.response=response;this.createdAt=now;this.updatedAt=now;}
    public enum Response { YES, NO, MAYBE }
    public void change(Response response,Instant now){this.response=response;this.updatedAt=now;}
    public UUID getClubId(){return clubId;} public UUID getMeetingId(){return meetingId;} public UUID getMembershipId(){return membershipId;} public Response getResponse(){return response;}
}
