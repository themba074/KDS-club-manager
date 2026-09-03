package com.kds.backend.members.application;
import java.util.UUID;
public record MeetingAudienceMember(UUID membershipId,String email,String displayName) {}
