package com.kds.backend.identity.repository;

import com.kds.backend.identity.application.MembershipLifecycleMember;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.identity.domain.ClubEntity;
import com.kds.backend.identity.domain.ClubMembershipEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipLifecycleRepository {
    private final EntityManager entityManager;
    public MembershipLifecycleRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void lockClub() {
        entityManager.createQuery("select c from ClubEntity c where c.id = :clubId", ClubEntity.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
    }

    public Optional<ClubMembershipEntity> membership(UUID membershipId) {
        return entityManager.createQuery("select m from ClubMembershipEntity m where m.id = :membershipId and m.club.id = :clubId", ClubMembershipEntity.class)
                .setParameter("membershipId", membershipId).setParameter("clubId", TenantContext.requireClubId())
                .getResultList().stream().findFirst();
    }

    public List<MembershipLifecycleMember> memberships() {
        return entityManager.createQuery("""
                select new com.kds.backend.identity.application.MembershipLifecycleMember(m.id, m.userId, m.roleCode, m.status)
                from ClubMembershipEntity m where m.club.id = :clubId
                """, MembershipLifecycleMember.class)
                .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }
}
