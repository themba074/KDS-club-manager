package com.kds.backend.contributions.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="contribution_payments")
public class ContributionPaymentEntity {
    @Id private UUID id;
    @Column(name="club_id",nullable=false) private UUID clubId;
    @Column(name="schedule_version_id",nullable=false) private UUID scheduleVersionId;
    @Column(name="membership_id",nullable=false) private UUID membershipId;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="received_on",nullable=false) private LocalDate receivedOn;
    @Column(length=120) private String reference;
    @Column(length=500) private String note;
    @Column(name="proof_storage_key",length=500) private String proofStorageKey;
    @Column(name="proof_file_name",length=255) private String proofFileName;
    @Column(name="proof_content_type",length=120) private String proofContentType;
    @Column(name="recorded_by",nullable=false) private UUID recordedBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ContributionPaymentEntity() {}
    public ContributionPaymentEntity(UUID id,UUID clubId,UUID scheduleVersionId,UUID membershipId,LocalDate dueDate,
            BigDecimal amount,String currency,LocalDate receivedOn,String reference,String note,UUID recordedBy,Instant createdAt) {
        this.id=id;this.clubId=clubId;this.scheduleVersionId=scheduleVersionId;this.membershipId=membershipId;
        this.dueDate=dueDate;this.amount=amount;this.currency=currency;this.receivedOn=receivedOn;
        this.reference=reference;this.note=note;this.recordedBy=recordedBy;this.createdAt=createdAt;
    }
    public void attachProof(String storageKey,String fileName,String contentType){this.proofStorageKey=storageKey;this.proofFileName=fileName;this.proofContentType=contentType;}
    public UUID getId(){return id;} public UUID getClubId(){return clubId;} public UUID getScheduleVersionId(){return scheduleVersionId;}
    public UUID getMembershipId(){return membershipId;} public LocalDate getDueDate(){return dueDate;} public BigDecimal getAmount(){return amount;}
    public String getCurrency(){return currency;} public LocalDate getReceivedOn(){return receivedOn;} public String getReference(){return reference;}
    public String getNote(){return note;} public String getProofStorageKey(){return proofStorageKey;} public String getProofFileName(){return proofFileName;}
    public String getProofContentType(){return proofContentType;} public UUID getRecordedBy(){return recordedBy;} public Instant getCreatedAt(){return createdAt;}
}
