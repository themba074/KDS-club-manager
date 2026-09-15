package com.kds.backend.audit.repository;

import com.kds.backend.audit.domain.AuditAction;
import com.kds.backend.audit.domain.AuditLogEntity;
import com.kds.backend.identity.application.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogRepository {
    private final EntityManager entityManager;

    public AuditLogRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void append(AuditLogEntity entry) {
        if (!TenantContext.requireClubId().equals(entry.getClubId())) throw new AccessDeniedException("Wrong club.");
        entityManager.persist(entry);
    }

    public List<AuditLogEntity> find(UUID actorId, AuditAction action, Instant from, Instant until, int page, int size) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(AuditLogEntity.class);
        Root<AuditLogEntity> root = query.from(AuditLogEntity.class);
        query.where(filters(builder, root, actorId, action, from, until));
        query.orderBy(builder.desc(root.get("occurredAt")), builder.desc(root.get("id")));
        return entityManager.createQuery(query).setFirstResult(Math.multiplyExact(page, size)).setMaxResults(size).getResultList();
    }

    public long count(UUID actorId, AuditAction action, Instant from, Instant until) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        Root<AuditLogEntity> root = query.from(AuditLogEntity.class);
        query.select(builder.count(root)).where(filters(builder, root, actorId, action, from, until));
        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] filters(CriteriaBuilder builder, Root<AuditLogEntity> root, UUID actorId,
                                AuditAction action, Instant from, Instant until) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("clubId"), TenantContext.requireClubId()));
        if (actorId != null) predicates.add(builder.equal(root.get("actorId"), actorId));
        if (action != null) predicates.add(builder.equal(root.get("action"), action));
        if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
        if (until != null) predicates.add(builder.lessThan(root.get("occurredAt"), until));
        return predicates.toArray(Predicate[]::new);
    }
}
