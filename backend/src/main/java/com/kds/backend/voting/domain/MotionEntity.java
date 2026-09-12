package com.kds.backend.voting.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "motions")
public class MotionEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 4000) private String description;
    @Column(name = "opens_at", nullable = false) private Instant opensAt;
    @Column(name = "closes_at", nullable = false) private Instant closesAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancelled_by") private UUID cancelledBy;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "results_published_at") private Instant resultsPublishedAt;
    @Column(name = "results_published_by") private UUID resultsPublishedBy;
    @Column(name = "result_outcome", length = 30) private String resultOutcome;
    @Column(name = "winning_option_id") private UUID winningOptionId;
    @Column(name = "total_votes") private Integer totalVotes;
    @Version private long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "motion_id", nullable = false)
    @OrderBy("position asc")
    private Set<MotionOptionEntity> options = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "motion_id", nullable = false)
    private Set<MotionEligibleMembershipEntity> eligibleMemberships = new LinkedHashSet<>();

    protected MotionEntity() {}
    public MotionEntity(UUID id, UUID clubId, UUID actor, Instant now) {
        this.id = id;
        this.clubId = clubId;
        createdBy = actor;
        createdAt = now;
        updatedAt = now;
    }

    public void update(String title, String description, Instant opensAt, Instant closesAt,
                       List<String> labels, Set<UUID> eligible, Instant now) {
        this.title = title;
        this.description = description;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        updatedAt = now;
        List<MotionOptionEntity> ordered = getOptions();
        options.removeIf(option -> option.getPosition() >= labels.size());
        for (int position = 0; position < labels.size(); position++) {
            if (position < ordered.size()) ordered.get(position).update(labels.get(position));
            else options.add(new MotionOptionEntity(UUID.randomUUID(), id, clubId, position, labels.get(position)));
        }
        // Retain unchanged rows: inserting replacements before orphan deletion violates the unique constraint.
        eligibleMemberships.removeIf(existing -> !eligible.contains(existing.getMembershipId()));
        Set<UUID> retained = getEligibleMembershipIds();
        eligible.stream().filter(membership -> !retained.contains(membership))
                .forEach(membership -> eligibleMemberships.add(new MotionEligibleMembershipEntity(id, membership, clubId)));
    }

    public void cancel(UUID actor, Instant now) { cancelledBy = actor; cancelledAt = now; updatedAt = now; }
    public void publishResults(UUID actor, Instant now, String outcome, UUID winner, int votes) {
        if (resultsPublishedAt != null) throw new IllegalStateException("Results are already published.");
        resultsPublishedBy = actor;
        resultsPublishedAt = now;
        resultOutcome = outcome;
        winningOptionId = winner;
        totalVotes = votes;
        updatedAt = now;
    }
    public MotionState stateAt(Instant instant) {
        if (cancelledAt != null) return MotionState.CANCELLED;
        if (instant.isBefore(opensAt)) return MotionState.DRAFT;
        return instant.isBefore(closesAt) ? MotionState.OPEN : MotionState.CLOSED;
    }
    public UUID getId() { return id; }
    public UUID getClubId() { return clubId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getOpensAt() { return opensAt; }
    public Instant getClosesAt() { return closesAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getResultsPublishedAt() { return resultsPublishedAt; }
    public String getResultOutcome() { return resultOutcome; }
    public UUID getWinningOptionId() { return winningOptionId; }
    public Integer getTotalVotes() { return totalVotes; }
    public long getVersion() { return version; }
    public List<MotionOptionEntity> getOptions() {
        return options.stream().sorted(Comparator.comparingInt(MotionOptionEntity::getPosition)).toList();
    }
    public Set<UUID> getEligibleMembershipIds() {
        return eligibleMemberships.stream().map(MotionEligibleMembershipEntity::getMembershipId).collect(Collectors.toUnmodifiableSet());
    }
}
