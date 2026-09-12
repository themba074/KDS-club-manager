package com.kds.backend.voting.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.voting.domain.MotionResultOptionEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MotionResultRepository {
    private final EntityManager entityManager;
    public MotionResultRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void addAll(List<MotionResultOptionEntity> results) { results.forEach(entityManager::persist); }

    public List<MotionResultOptionEntity> options(UUID motionId) {
        return entityManager.createQuery("""
                select result from MotionResultOptionEntity result
                where result.clubId = :clubId and result.motionId = :motionId order by result.position
                """, MotionResultOptionEntity.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("motionId", motionId)
                .getResultList();
    }
}
