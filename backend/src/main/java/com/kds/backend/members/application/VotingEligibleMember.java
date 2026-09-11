package com.kds.backend.members.application;
import java.util.UUID;
public record VotingEligibleMember(UUID membershipId,String displayName,String email) {}
