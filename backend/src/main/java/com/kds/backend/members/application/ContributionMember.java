package com.kds.backend.members.application;
import java.util.UUID;
public record ContributionMember(UUID membershipId, String email, String displayName, boolean active) {
    public ContributionMember(UUID membershipId,String email,String displayName){this(membershipId,email,displayName,true);}
    public ContributionMember withoutStatus(){return new ContributionMember(membershipId,email,displayName);}
}
