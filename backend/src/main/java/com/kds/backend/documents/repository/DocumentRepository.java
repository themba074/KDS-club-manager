package com.kds.backend.documents.repository;

import com.kds.backend.documents.domain.DocumentEntity;
import com.kds.backend.identity.application.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepository {
    private final EntityManager entityManager;

    public DocumentRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void add(DocumentEntity document) {
        if (!document.getClubId().equals(TenantContext.requireClubId())) {
            throw new AccessDeniedException("Wrong club.");
        }
        entityManager.persist(document);
    }

    public List<DocumentEntity> all() {
        return entityManager.createQuery("""
                select distinct document from DocumentEntity document
                left join fetch document.visibilityRoles
                left join fetch document.versions
                where document.clubId = :clubId
                order by document.updatedAt desc, document.id
                """, DocumentEntity.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .getResultList();
    }

    public Optional<DocumentEntity> find(UUID id) {
        return fetchQuery(id).getResultList().stream().findFirst();
    }

    public Optional<DocumentEntity> lock(UUID id) {
        Optional<DocumentEntity> locked = entityManager.createQuery("""
                select document from DocumentEntity document
                where document.clubId = :clubId and document.id = :id
                """, DocumentEntity.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().stream().findFirst();
        locked.ifPresent(document -> {
            document.getVisibleRoleCodes();
            document.getVersions();
        });
        return locked;
    }

    public void flush() { entityManager.flush(); }

    private jakarta.persistence.TypedQuery<DocumentEntity> fetchQuery(UUID id) {
        return entityManager.createQuery("""
                select distinct document from DocumentEntity document
                left join fetch document.visibilityRoles
                left join fetch document.versions
                where document.clubId = :clubId and document.id = :id
                """, DocumentEntity.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setParameter("id", id);
    }
}
