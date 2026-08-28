package com.kds.backend.members.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_profiles")
public class MemberProfileEntity {
    @Id @Column(name = "membership_id") private UUID membershipId;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "first_name", nullable = false, length = 80) private String firstName;
    @Column(name = "last_name", nullable = false, length = 80) private String lastName;
    @Column(length = 30) private String phone;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MemberProfileEntity() {}
    public MemberProfileEntity(UUID membershipId, UUID clubId, String firstName, String lastName, String phone, Instant createdAt) {
        this.membershipId = membershipId;
        this.clubId = clubId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.createdAt = createdAt;
    }
    public UUID getMembershipId() { return membershipId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
}
