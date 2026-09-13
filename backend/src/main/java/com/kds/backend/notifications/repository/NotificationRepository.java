package com.kds.backend.notifications.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.notifications.domain.NotificationEntity;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public class NotificationRepository {
    private final EntityManager entityManager;
    public NotificationRepository(EntityManager entityManager){this.entityManager=entityManager;}
    public void add(NotificationEntity notification){if(!notification.getClubId().equals(TenantContext.requireClubId()))throw new AccessDeniedException("Wrong club.");entityManager.persist(notification);}
    public boolean exists(String key){return !entityManager.createQuery("select n.id from NotificationEntity n where n.clubId=:club and n.deduplicationKey=:key",UUID.class).setParameter("club",TenantContext.requireClubId()).setParameter("key",key).setMaxResults(1).getResultList().isEmpty();}
    public List<NotificationEntity> feed(UUID membership,Instant now){return entityManager.createQuery("select n from NotificationEntity n where n.clubId=:club and n.membershipId=:member and n.cancelledAt is null and n.availableAt<=:now order by n.availableAt desc,n.id",NotificationEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("member",membership).setParameter("now",now).setMaxResults(100).getResultList();}
    public long unreadCount(UUID membership,Instant now){return entityManager.createQuery("select count(n) from NotificationEntity n where n.clubId=:club and n.membershipId=:member and n.cancelledAt is null and n.availableAt<=:now and n.readAt is null",Long.class).setParameter("club",TenantContext.requireClubId()).setParameter("member",membership).setParameter("now",now).getSingleResult();}
    public Optional<NotificationEntity> find(UUID id,UUID membership,Instant now){return entityManager.createQuery("select n from NotificationEntity n where n.clubId=:club and n.membershipId=:member and n.id=:id and n.cancelledAt is null and n.availableAt<=:now",NotificationEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("member",membership).setParameter("id",id).setParameter("now",now).getResultList().stream().findFirst();}
    public int markAllRead(UUID membership,Instant now){return entityManager.createQuery("update NotificationEntity n set n.readAt=:now where n.clubId=:club and n.membershipId=:member and n.readAt is null and n.cancelledAt is null and n.availableAt<=:now").setParameter("club",TenantContext.requireClubId()).setParameter("member",membership).setParameter("now",now).executeUpdate();}
    public List<NotificationEntity> dueEmails(Instant now,int maxAttempts){return entityManager.createQuery("select n from NotificationEntity n where n.clubId=:club and n.cancelledAt is null and n.availableAt<=:now and n.emailedAt is null and n.emailAttempts<:attempts order by n.availableAt,n.id",NotificationEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("now",now).setParameter("attempts",maxAttempts).setMaxResults(100).getResultList();}
    public int deletePending(String sourceType,UUID sourceId,Instant now){return entityManager.createQuery("delete from NotificationEntity n where n.clubId=:club and n.sourceType=:type and n.sourceId=:source and n.availableAt>:now and n.emailedAt is null").setParameter("club",TenantContext.requireClubId()).setParameter("type",sourceType).setParameter("source",sourceId).setParameter("now",now).executeUpdate();}
    public int cancelPending(String sourceType,UUID sourceId,Instant now){return entityManager.createQuery("update NotificationEntity n set n.cancelledAt=:now where n.clubId=:club and n.sourceType=:type and n.sourceId=:source and n.availableAt>:now and n.cancelledAt is null").setParameter("club",TenantContext.requireClubId()).setParameter("type",sourceType).setParameter("source",sourceId).setParameter("now",now).executeUpdate();}
    public void flush(){entityManager.flush();}
}
