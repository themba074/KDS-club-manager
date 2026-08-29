package com.kds.backend.contributions.repository;

import com.kds.backend.contributions.domain.*;
import com.kds.backend.identity.application.TenantContext;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ContributionScheduleRepository {
    private final EntityManager entityManager;
    public ContributionScheduleRepository(EntityManager entityManager) { this.entityManager=entityManager; }
    public void add(ContributionScheduleEntity schedule) {
        if (!schedule.getClubId().equals(TenantContext.requireClubId())) throw new org.springframework.security.access.AccessDeniedException("Wrong club.");
        entityManager.persist(schedule);
    }
    public void add(ContributionScheduleVersionEntity version) {
        if (!version.getClubId().equals(TenantContext.requireClubId())) throw new org.springframework.security.access.AccessDeniedException("Wrong club.");
        entityManager.persist(version);
    }
    public void addAssignments(UUID versionId,Set<UUID> membershipIds) {
        UUID club=TenantContext.requireClubId();
        membershipIds.forEach(member->entityManager.persist(new ContributionScheduleAssignmentEntity(versionId,club,member)));
    }
    public Set<UUID> assignments(UUID versionId) {
        return Set.copyOf(entityManager.createQuery("select a.id.membershipId from ContributionScheduleAssignmentEntity a where a.clubId=:club and a.id.scheduleVersionId=:version",UUID.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("version",versionId).getResultList());
    }
    public Optional<ContributionScheduleEntity> lockSchedule(UUID id) {
        return entityManager.createQuery("select s from ContributionScheduleEntity s where s.clubId=:club and s.id=:id",ContributionScheduleEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("id",id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst();
    }
    public Optional<ContributionScheduleVersionEntity> latest(UUID scheduleId) {
        return entityManager.createQuery("select v from ContributionScheduleVersionEntity v where v.clubId=:club and v.scheduleId=:id order by v.versionNumber desc",ContributionScheduleVersionEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("id",scheduleId).setMaxResults(1).getResultList().stream().findFirst();
    }
    public List<ContributionScheduleVersionEntity> latestVersions() {
        return entityManager.createQuery("select v from ContributionScheduleVersionEntity v where v.clubId=:club and v.versionNumber=(select max(v2.versionNumber) from ContributionScheduleVersionEntity v2 where v2.clubId=:club and v2.scheduleId=v.scheduleId) order by v.name",ContributionScheduleVersionEntity.class)
            .setParameter("club",TenantContext.requireClubId()).getResultList();
    }
    public List<ContributionScheduleVersionEntity> versionsOverlapping(java.time.LocalDate from, java.time.LocalDate to) {
        return entityManager.createQuery("select v from ContributionScheduleVersionEntity v where v.clubId=:club and v.effectiveFrom<=:to and (v.effectiveTo is null or v.effectiveTo>=:from) and v.firstDueDate<=:to and (v.endDate is null or v.endDate>=:from)",ContributionScheduleVersionEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("from",from).setParameter("to",to).getResultList();
    }
}
