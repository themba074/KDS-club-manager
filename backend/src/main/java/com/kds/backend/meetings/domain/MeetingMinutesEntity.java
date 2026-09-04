package com.kds.backend.meetings.domain;

import com.kds.backend.documents.application.StoredFile;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="meeting_minutes")
public class MeetingMinutesEntity {
    @Id private UUID id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(name="meeting_id",nullable=false) private UUID meetingId;
    @Column(length=20000) private String body;
    @Column(name="attachment_key",length=1000) private String attachmentKey;
    @Column(name="attachment_name",length=255) private String attachmentName;
    @Column(name="attachment_content_type",length=120) private String attachmentContentType;
    @Column(name="attachment_size") private Long attachmentSize;
    @Column(name="published_at") private Instant publishedAt;
    @Column(name="created_by",nullable=false) private UUID createdBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    protected MeetingMinutesEntity() {}
    public MeetingMinutesEntity(UUID id,UUID clubId,UUID meetingId,UUID actor,Instant now){this.id=id;this.clubId=clubId;this.meetingId=meetingId;this.createdBy=actor;this.createdAt=now;this.updatedAt=now;}
    public void updateBody(String body,Instant now){this.body=body;this.publishedAt=null;this.updatedAt=now;}
    public void attach(StoredFile file,Instant now){this.attachmentKey=file.storageKey();this.attachmentName=file.fileName();this.attachmentContentType=file.contentType();this.attachmentSize=file.size();this.publishedAt=null;this.updatedAt=now;}
    public void publish(Instant now){this.publishedAt=now;this.updatedAt=now;}
    public UUID getId(){return id;} public UUID getClubId(){return clubId;} public UUID getMeetingId(){return meetingId;} public String getBody(){return body;} public String getAttachmentKey(){return attachmentKey;} public String getAttachmentName(){return attachmentName;} public String getAttachmentContentType(){return attachmentContentType;} public Long getAttachmentSize(){return attachmentSize;} public Instant getPublishedAt(){return publishedAt;} public long getVersion(){return version;}
}
