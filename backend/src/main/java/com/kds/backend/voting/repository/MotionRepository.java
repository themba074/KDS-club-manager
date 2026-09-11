package com.kds.backend.voting.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.voting.domain.MotionEntity;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MotionRepository {
    private final EntityManager entityManager;
    public MotionRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void add(MotionEntity motion) {
        if (!motion.getClubId().equals(TenantContext.requireClubId())) throw new AccessDeniedException("Wrong club.");
        entityManager.persist(motion);
    }

    public Optional<MotionEntity> find(UUID id) {
        return entityManager.createQuery("""
                select distinct motion from MotionEntity motion
                left join fetch motion.options left join fetch motion.eligibleMemberships
                where motion.clubId = :clubId and motion.id = :id
                """, MotionEntity.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("id", id)
                .getResultList().stream().findFirst();
    }

    public List<MotionEntity> all() {
        return entityManager.createQuery("""
                select distinct motion from MotionEntity motion
                left join fetch motion.options left join fetch motion.eligibleMemberships
                where motion.clubId = :clubId order by motion.opensAt desc, motion.id
                """, MotionEntity.class)
                .setParameter("clubId", TenantContext.requireClubId()).getResultList();
    }

    public void flush() { entityManager.flush(); }
}
