package com.kds.backend.identity.repository;

import com.kds.backend.identity.domain.ClubEntity;
import com.kds.backend.identity.domain.ClubMembershipEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Identity bootstrap queries: every read is constrained to the authenticated user.
 * This is not a general-purpose tenant-data repository. */
@Repository
public class ClubAccessRepository {
    private final EntityManager entityManager;
    public ClubAccessRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void create(ClubEntity club, ClubMembershipEntity creator) {
        entityManager.persist(club);
        entityManager.persist(creator);
    }

    public List<ClubMembershipEntity> membershipsForUser(UUID userId) {
        return entityManager.createQuery("select m from ClubMembershipEntity m join fetch m.club where m.userId = :userId order by m.club.name, m.club.id", ClubMembershipEntity.class)
                .setParameter("userId", userId).getResultList();
    }

    public Optional<ClubMembershipEntity> membership(UUID userId, UUID clubId) {
        return entityManager.createQuery("select m from ClubMembershipEntity m join fetch m.club where m.userId = :userId and m.club.id = :clubId", ClubMembershipEntity.class)
                .setParameter("userId", userId).setParameter("clubId", clubId).getResultList().stream().findFirst();
    }
}
