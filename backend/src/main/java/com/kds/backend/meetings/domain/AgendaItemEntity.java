package com.kds.backend.meetings.domain;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="meeting_agenda_items")
public class AgendaItemEntity {
    @Id private UUID id;
    @Column(name="meeting_id",nullable=false,insertable=false,updatable=false) private UUID meetingId;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(nullable=false) private int position;
    @Column(nullable=false,length=200) private String title;
    @Column(length=2000) private String description;
    protected AgendaItemEntity() {}
    public AgendaItemEntity(UUID id,UUID meetingId,UUID clubId,int position,String title,String description){this.id=id;this.meetingId=meetingId;this.clubId=clubId;this.position=position;this.title=title;this.description=description;}
    public void update(String title,String description){this.title=title;this.description=description;}
    public UUID getId(){return id;} public int getPosition(){return position;} public String getTitle(){return title;} public String getDescription(){return description;}
}
