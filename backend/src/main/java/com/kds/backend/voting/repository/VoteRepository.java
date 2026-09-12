package com.kds.backend.voting.repository;

import com.kds.backend.identity.application.TenantContext;
import com.kds.backend.voting.domain.VoteEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class VoteRepository {
    public record OptionCount(UUID optionId, long count) {}
    public record Selection(UUID motionId, UUID optionId) {}

    private final EntityManager entityManager;
    public VoteRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void add(VoteEntity vote) { entityManager.persist(vote); }

    public boolean exists(UUID motionId, UUID membershipId) {
        return entityManager.createQuery("""
                select count(vote) from VoteEntity vote
                where vote.clubId = :clubId and vote.motionId = :motionId and vote.membershipId = :membershipId
                """, Long.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setParameter("motionId", motionId).setParameter("membershipId", membershipId)
                .getSingleResult() > 0;
    }

    public boolean hasVotes(UUID motionId) {
        return entityManager.createQuery("""
                select count(vote) from VoteEntity vote
                where vote.clubId = :clubId and vote.motionId = :motionId
                """, Long.class)
                .setParameter("clubId", TenantContext.requireClubId())
                .setParameter("motionId", motionId)
                .getSingleResult() > 0;
    }

    public Map<UUID, UUID> selections(UUID membershipId) {
        return entityManager.createQuery("""
                select new com.kds.backend.voting.repository.VoteRepository$Selection(vote.motionId, vote.optionId)
                from VoteEntity vote where vote.clubId = :clubId and vote.membershipId = :membershipId
                """, Selection.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("membershipId", membershipId)
                .getResultList().stream().collect(Collectors.toMap(Selection::motionId, Selection::optionId));
    }

    public List<OptionCount> counts(UUID motionId) {
        return entityManager.createQuery("""
                select new com.kds.backend.voting.repository.VoteRepository$OptionCount(vote.optionId, count(vote))
                from VoteEntity vote where vote.clubId = :clubId and vote.motionId = :motionId
                group by vote.optionId
                """, OptionCount.class)
                .setParameter("clubId", TenantContext.requireClubId()).setParameter("motionId", motionId)
                .getResultList();
    }

    public void flush() { entityManager.flush(); }
}
