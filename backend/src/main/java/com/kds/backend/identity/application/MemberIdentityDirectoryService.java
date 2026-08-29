package com.kds.backend.identity.application;

import com.kds.backend.identity.repository.MemberIdentityDirectoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Public Identity read interface used by the Members module. */
@Service
@Transactional(readOnly = true)
public class MemberIdentityDirectoryService {
    private final MemberIdentityDirectoryRepository repository;
    public MemberIdentityDirectoryService(MemberIdentityDirectoryRepository repository) { this.repository = repository; }
    public List<IdentityDirectoryMember> activeMembers() { return repository.activeMembers(); }
    public boolean membershipEmailExists(String email) { return repository.membershipEmailExists(email); }
    public Set<String> membershipEmails(Set<String> emails) { return repository.membershipEmails(emails); }
}
