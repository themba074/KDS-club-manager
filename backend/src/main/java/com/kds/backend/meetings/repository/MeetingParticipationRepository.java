package com.kds.backend.meetings.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.meetings.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class MeetingParticipationRepository {
    private final EntityManager entityManager;
    public MeetingParticipationRepository(EntityManager entityManager){this.entityManager=entityManager;}
    public Optional<MeetingRsvpEntity> rsvp(UUID meetingId,UUID membershipId){return entityManager.createQuery("select r from MeetingRsvpEntity r where r.clubId=:club and r.meetingId=:meeting and r.membershipId=:membership",MeetingRsvpEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("meeting",meetingId).setParameter("membership",membershipId).getResultList().stream().findFirst();}
    public Map<MeetingRsvpEntity.Response,Long> counts(UUID meetingId){var rows=entityManager.createQuery("select r.response,count(r) from MeetingRsvpEntity r where r.clubId=:club and r.meetingId=:meeting group by r.response",Object[].class).setParameter("club",TenantContext.requireClubId()).setParameter("meeting",meetingId).getResultList();Map<MeetingRsvpEntity.Response,Long> result=new EnumMap<>(MeetingRsvpEntity.Response.class);rows.forEach(row->result.put((MeetingRsvpEntity.Response)row[0],(Long)row[1]));return result;}
    public void add(MeetingRsvpEntity value){guard(value.getClubId());entityManager.persist(value);}
    public Optional<MeetingMinutesEntity> minutes(UUID meetingId){return entityManager.createQuery("select m from MeetingMinutesEntity m where m.clubId=:club and m.meetingId=:meeting",MeetingMinutesEntity.class).setParameter("club",TenantContext.requireClubId()).setParameter("meeting",meetingId).getResultList().stream().findFirst();}
    public void add(MeetingMinutesEntity value){guard(value.getClubId());entityManager.persist(value);}
    public void flush(){entityManager.flush();}
    private static void guard(UUID clubId){if(!TenantContext.requireClubId().equals(clubId))throw new AccessDeniedException("Wrong club.");}
}
