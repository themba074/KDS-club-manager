package com.kds.backend.members.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.members.application.MemberDirectoryEntry;
import com.kds.backend.members.domain.MemberInvitationEntity;
import com.kds.backend.members.domain.MemberProfileEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberRepository {
    private final EntityManager entityManager;
    public MemberRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void saveInvitation(MemberInvitationEntity invitation) { entityManager.persist(invitation); }
    public void saveProfile(MemberProfileEntity profile) { entityManager.persist(profile); }

    public boolean invitationEmailExists(String email) {
        return !entityManager.createQuery("select i.id from MemberInvitationEntity i where i.clubId = :clubId and i.email = :email", UUID.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("email", email)
                .setMaxResults(1).getResultList().isEmpty();
    }

    public Optional<MemberInvitationEntity> lockByTokenHash(String tokenHash) {
        return entityManager.createQuery("select i from MemberInvitationEntity i where i.tokenHash = :tokenHash", MemberInvitationEntity.class)
                .setParameter("tokenHash", tokenHash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().stream().findFirst();
    }

    public Optional<MemberInvitationEntity> byTokenHash(String tokenHash) {
        return entityManager.createQuery("select i from MemberInvitationEntity i where i.tokenHash = :tokenHash", MemberInvitationEntity.class)
                .setParameter("tokenHash", tokenHash).getResultList().stream().findFirst();
    }

    public List<MemberProfileEntity> profiles() {
        return entityManager.createQuery("select p from MemberProfileEntity p where p.clubId = :clubId", MemberProfileEntity.class)
                .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }

    public List<MemberDirectoryEntry> pendingInvitations() {
        return entityManager.createQuery("""
                select new com.kds.backend.members.application.MemberDirectoryEntry(
                    i.id, i.email, i.firstName, i.lastName, i.phone, i.roleCode,
                    com.kds.backend.members.application.MemberDirectoryEntry$MemberStatus.INVITED, i.createdAt)
                from MemberInvitationEntity i
                where i.clubId = :clubId and i.acceptedAt is null
                order by i.email
                """, MemberDirectoryEntry.class)
                .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }
}
