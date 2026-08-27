package com.kds.backend.identity.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.identity.domain.ClubEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped repository: no unrestricted findAll/findById escape hatch. */
@Repository
public class CurrentClubRepository {
    private final EntityManager entityManager;
    public CurrentClubRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public Optional<ClubEntity> findById(UUID requestedId) {
        return entityManager.createQuery("select c from ClubEntity c where c.id = :requestedId and c.id = :tenantId", ClubEntity.class)
                .setParameter("requestedId", requestedId).setParameter("tenantId", TenantContext.requireClubId())
                .getResultList().stream().findFirst();
    }
}
