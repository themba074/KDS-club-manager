package com.kds.backend.identity.repository;
import com.kds.backend.identity.application.RoleMember;
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
public class RoleMembershipRepository {
    private final EntityManager entityManager;
    public RoleMembershipRepository(EntityManager entityManager) { this.entityManager = entityManager; }
    public void lockClub() {
        entityManager.createQuery("select c from ClubEntity c where c.id = :clubId", ClubEntity.class)
            .setParameter("clubId", TenantContext.requireClubId()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
    }
    public List<RoleMember> members() {
        return entityManager.createQuery("select new com.kds.backend.identity.application.RoleMember(m.id, u.email, m.roleCode) from ClubMembershipEntity m, UserEntity u where m.userId = u.id and m.club.id = :clubId and m.status = 'ACTIVE' order by u.email", RoleMember.class)
            .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }
    public Optional<ClubMembershipEntity> membership(UUID membershipId) {
        return entityManager.createQuery("select m from ClubMembershipEntity m join fetch m.club where m.id = :id and m.club.id = :clubId", ClubMembershipEntity.class)
            .setParameter("id", membershipId).setParameter("clubId", TenantContext.requireClubId()).getResultList().stream().findFirst();
    }
}

