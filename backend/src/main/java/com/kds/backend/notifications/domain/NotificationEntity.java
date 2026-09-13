package com.kds.backend.notifications.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notifications")
public class NotificationEntity {
    @Id private UUID id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(name="membership_id",nullable=false) private UUID membershipId;
    @Column(name="recipient_email",nullable=false,length=320) private String recipientEmail;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private NotificationType type;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,length=1000) private String message;
    @Column(name="target_path",length=500) private String targetPath;
    @Column(name="source_type",nullable=false,length=40) private String sourceType;
    @Column(name="source_id",nullable=false) private UUID sourceId;
    @Column(name="deduplication_key",nullable=false,length=500) private String deduplicationKey;
    @Column(name="available_at",nullable=false) private Instant availableAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="read_at") private Instant readAt;
    @Column(name="email_attempts",nullable=false) private int emailAttempts;
    @Column(name="email_attempted_at") private Instant emailAttemptedAt;
    @Column(name="emailed_at") private Instant emailedAt;
    @Column(name="email_error",length=1000) private String emailError;
    @Column(name="cancelled_at") private Instant cancelledAt;

    protected NotificationEntity() {}
    public NotificationEntity(UUID id,UUID clubId,UUID membershipId,String recipientEmail,NotificationType type,
            String title,String message,String targetPath,String sourceType,UUID sourceId,String deduplicationKey,
            Instant availableAt,Instant createdAt){
        this.id=id;this.clubId=clubId;this.membershipId=membershipId;this.recipientEmail=recipientEmail;
        this.type=type;this.title=title;this.message=message;this.targetPath=targetPath;this.sourceType=sourceType;
        this.sourceId=sourceId;this.deduplicationKey=deduplicationKey;this.availableAt=availableAt;this.createdAt=createdAt;
    }
    public void markRead(Instant now){if(readAt==null)readAt=now;}
    public void markEmailDelivered(Instant now){emailAttempts++;emailAttemptedAt=now;emailedAt=now;emailError=null;}
    public void markEmailFailed(Instant now,String error){emailAttempts++;emailAttemptedAt=now;emailError=error==null?"Delivery failed":error.substring(0,Math.min(error.length(),1000));}
    public void cancel(Instant now){cancelledAt=now;}
    public UUID getId(){return id;} public UUID getClubId(){return clubId;} public UUID getMembershipId(){return membershipId;}
    public String getRecipientEmail(){return recipientEmail;} public NotificationType getType(){return type;}
    public String getTitle(){return title;} public String getMessage(){return message;} public String getTargetPath(){return targetPath;}
    public String getSourceType(){return sourceType;} public UUID getSourceId(){return sourceId;} public String getDeduplicationKey(){return deduplicationKey;}
    public Instant getAvailableAt(){return availableAt;} public Instant getCreatedAt(){return createdAt;} public Instant getReadAt(){return readAt;}
    public int getEmailAttempts(){return emailAttempts;} public Instant getEmailAttemptedAt(){return emailAttemptedAt;}
    public Instant getEmailedAt(){return emailedAt;} public String getEmailError(){return emailError;} public Instant getCancelledAt(){return cancelledAt;}
}
