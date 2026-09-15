package com.kds.backend.audit.application;

import com.kds.backend.audit.domain.AuditAction;
import com.kds.backend.audit.domain.AuditLogEntity;
import com.kds.backend.audit.repository.AuditLogRepository;
import com.kds.backend.clubtypeconfig.application.Permission;
import com.kds.backend.identity.application.ClubService;
import com.kds.backend.identity.application.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;
    private final ClubService clubs;
    private final Clock clock;

    public AuditLogService(AuditLogRepository repository, ClubService clubs, Clock clock) {
        this.repository = repository;
        this.clubs = clubs;
        this.clock = clock;
    }

    /** Append in the caller's transaction so the business action and its evidence commit together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, AuditAction action, String entityType, UUID entityId,
                       String previousValue, String newValue) {
        Objects.requireNonNull(actorId);
        Objects.requireNonNull(action);
        Objects.requireNonNull(entityType);
        Objects.requireNonNull(entityId);
        repository.append(new AuditLogEntity(UUID.randomUUID(), TenantContext.requireClubId(), actorId, action,
                entityType, entityId, previousValue, newValue, clock.instant()));
    }

    @Transactional(readOnly = true)
    public AuditLogPage search(UUID actorId, UUID filterActor, AuditAction action, LocalDate from, LocalDate to,
                               int page, int size) {
        var club = clubs.requireMembership(actorId, TenantContext.requireClubId());
        if (!club.permissions().contains(Permission.AUDIT_READ.name()))
            throw new AccessDeniedException("You do not have permission to read the audit log.");
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid page and size of at most 100.");
        if (from != null && to != null && (to.isBefore(from) || to.isAfter(from.plusYears(1))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a date range of at most one year.");
        var start = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        var until = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long total = repository.count(filterActor, action, start, until);
        var content = repository.find(filterActor, action, start, until, page, size).stream()
                .map(entry -> new AuditLogView(entry.getId(), entry.getActorId(), entry.getAction(),
                        entry.getEntityType(), entry.getEntityId(), entry.getPreviousValue(),
                        entry.getNewValue(), entry.getOccurredAt())).toList();
        int pages = Math.toIntExact((total + size - 1) / size);
        return new AuditLogPage(content, total, pages, page, size);
    }
}
