package com.kds.backend.contributions.repository;

import com.kds.backend.contributions.domain.ContributionPaymentEntity;
import com.kds.backend.identity.application.TenantContext;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
public class ContributionPaymentRepository {
    private final EntityManager entityManager;
    public ContributionPaymentRepository(EntityManager entityManager){this.entityManager=entityManager;}
    public void add(ContributionPaymentEntity payment){
        if(!payment.getClubId().equals(TenantContext.requireClubId()))throw new AccessDeniedException("Wrong club.");
        entityManager.persist(payment);
    }
    public List<ContributionPaymentEntity> forMembership(UUID membershipId,LocalDate from,LocalDate to){
        return entityManager.createQuery("select p from ContributionPaymentEntity p where p.clubId=:club and p.membershipId=:member and p.dueDate between :from and :to order by p.receivedOn,p.createdAt,p.id",ContributionPaymentEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("member",membershipId)
            .setParameter("from",from).setParameter("to",to).getResultList();
    }
    public List<ContributionPaymentEntity> forExpectation(UUID versionId,UUID membershipId,LocalDate dueDate){
        return entityManager.createQuery("select p from ContributionPaymentEntity p where p.clubId=:club and p.scheduleVersionId=:version and p.membershipId=:member and p.dueDate=:due order by p.createdAt,p.id",ContributionPaymentEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("version",versionId)
            .setParameter("member",membershipId).setParameter("due",dueDate).getResultList();
    }
    public List<ContributionPaymentEntity> forPeriod(LocalDate from,LocalDate to){
        return entityManager.createQuery("select p from ContributionPaymentEntity p where p.clubId=:club and p.dueDate between :from and :to order by p.membershipId,p.dueDate,p.createdAt,p.id",ContributionPaymentEntity.class)
            .setParameter("club",TenantContext.requireClubId()).setParameter("from",from).setParameter("to",to).getResultList();
    }
}
