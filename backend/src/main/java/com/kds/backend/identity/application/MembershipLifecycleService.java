package com.kds.backend.identity.application;

import com.kds.backend.identity.repository.MembershipLifecycleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Public Identity mutation boundary used by Members for tenant membership lifecycle changes. */
@Service
@Transactional(readOnly = true)
public class MembershipLifecycleService {
    private final MembershipLifecycleRepository repository;
    public MembershipLifecycleService(MembershipLifecycleRepository repository) { this.repository = repository; }

    public void lockClub() { repository.lockClub(); }
    public MembershipLifecycleMember requireMembership(UUID membershipId) {
        var membership = repository.membership(membershipId)
                .orElseThrow(() -> new AccessDeniedException("Membership is unavailable in this club."));
        return new MembershipLifecycleMember(membership.getId(), membership.getUserId(), membership.getRoleCode(), membership.getStatus());
    }
    public List<MembershipLifecycleMember> memberships() { return repository.memberships(); }

    @Transactional
    public void changeStatus(UUID membershipId, String status) {
        repository.membership(membershipId)
                .orElseThrow(() -> new AccessDeniedException("Membership is unavailable in this club."))
                .changeStatus(status);
    }
}
