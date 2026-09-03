package com.kds.backend.meetings.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="meetings")
public class MeetingEntity {
    @Id private UUID id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(nullable=false,length=160) private String title;
    @Column(length=4000) private String description;
    @Column(name="starts_at",nullable=false) private Instant startsAt;
    @Column(name="utc_offset_minutes",nullable=false) private int utcOffsetMinutes;
    @Column(name="duration_minutes",nullable=false) private int durationMinutes;
    @Column(length=240) private String location;
    @Column(name="meeting_url",length=500) private String meetingUrl;
    @Column(name="created_by",nullable=false) private UUID createdBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true)
    @JoinColumn(name="meeting_id",nullable=false)
    @OrderBy("position asc") private List<AgendaItemEntity> agendaItems=new ArrayList<>();
    protected MeetingEntity() {}
    public MeetingEntity(UUID id,UUID clubId,UUID actor,Instant now){this.id=id;this.clubId=clubId;this.createdBy=actor;this.createdAt=now;this.updatedAt=now;}
    public void update(String title,String description,Instant startsAt,int utcOffsetMinutes,int durationMinutes,String location,String meetingUrl,List<AgendaDraft> agenda,Instant now){
        this.title=title;this.description=description;this.startsAt=startsAt;this.utcOffsetMinutes=utcOffsetMinutes;this.durationMinutes=durationMinutes;this.location=location;this.meetingUrl=meetingUrl;this.updatedAt=now;
        while(agendaItems.size()>agenda.size())agendaItems.remove(agendaItems.size()-1);
        for(int position=0;position<agenda.size();position++){var item=agenda.get(position);if(position<agendaItems.size())agendaItems.get(position).update(item.title(),item.description());else agendaItems.add(new AgendaItemEntity(UUID.randomUUID(),id,clubId,position,item.title(),item.description()));}
    }
    public record AgendaDraft(String title,String description) {}
    public UUID getId(){return id;} public UUID getClubId(){return clubId;} public String getTitle(){return title;} public String getDescription(){return description;}
    public Instant getStartsAt(){return startsAt;} public int getUtcOffsetMinutes(){return utcOffsetMinutes;} public int getDurationMinutes(){return durationMinutes;}
    public String getLocation(){return location;} public String getMeetingUrl(){return meetingUrl;} public long getVersion(){return version;} public List<AgendaItemEntity> getAgendaItems(){return List.copyOf(agendaItems);}
}
