package com.kds.backend.members.application;

public interface MemberInvitationDelivery {
    void deliver(String email, String rawToken);
}
