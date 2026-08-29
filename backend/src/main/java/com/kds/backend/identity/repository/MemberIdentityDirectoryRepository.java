package com.kds.backend.identity.repository;

import com.kds.backend.identity.application.IdentityDirectoryMember;
import com.kds.backend.identity.application.TenantContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class MemberIdentityDirectoryRepository {
    private final EntityManager entityManager;
    public MemberIdentityDirectoryRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public List<IdentityDirectoryMember> activeMembers() {
        return entityManager.createQuery("""
                select new com.kds.backend.identity.application.IdentityDirectoryMember(m.id, u.email, m.roleCode, m.status, m.createdAt)
                from ClubMembershipEntity m, UserEntity u
                where m.userId = u.id and m.club.id = :clubId
                order by u.email
                """, IdentityDirectoryMember.class)
                .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }

    public boolean membershipEmailExists(String email) {
        return !entityManager.createQuery("select m.id from ClubMembershipEntity m, UserEntity u where m.userId = u.id and m.club.id = :clubId and u.email = :email", UUID.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("email", email)
                .setMaxResults(1).getResultList().isEmpty();
    }

    public Set<String> membershipEmails(Set<String> emails) {
        if (emails.isEmpty()) return Set.of();
        return Set.copyOf(entityManager.createQuery("""
                select u.email from ClubMembershipEntity m, UserEntity u
                where m.userId = u.id and m.club.id = :clubId and u.email in :emails
                """, String.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setParameter("emails", emails)
                .getResultList());
    }
}
