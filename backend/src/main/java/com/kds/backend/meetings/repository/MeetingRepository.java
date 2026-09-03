package com.kds.backend.meetings.repository;
import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.meetings.domain.MeetingEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
@Repository
public class MeetingRepository {
    private final EntityManager entityManager;
    public MeetingRepository(EntityManager entityManager){this.entityManager=entityManager;}
    public void add(MeetingEntity meeting){if(!meeting.getClubId().equals(TenantContext.requireClubId()))throw new org.springframework.security.access.AccessDeniedException("Wrong club.");entityManager.persist(meeting);}
    public Optional<MeetingEntity> find(UUID id){return entityManager.createQuery("select distinct m from MeetingEntity m left join fetch m.agendaItems where m.clubId=:club and m.id=:id",MeetingEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("id",id).getResultList().stream().findFirst();}
    public List<MeetingEntity> upcoming(Instant now){return entityManager.createQuery("select distinct m from MeetingEntity m left join fetch m.agendaItems where m.clubId=:club and m.startsAt>=:now order by m.startsAt,m.id",MeetingEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("now",now).getResultList();}
    public List<MeetingEntity> past(Instant now){return entityManager.createQuery("select distinct m from MeetingEntity m left join fetch m.agendaItems where m.clubId=:club and m.startsAt<:now order by m.startsAt desc,m.id",MeetingEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("now",now).getResultList();}
    public void flush(){entityManager.flush();}
}
