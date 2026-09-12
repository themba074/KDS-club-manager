package com.kds.backend.voting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.kds.backend.voting.domain.MotionState;

public record MotionView(UUID id, long version, String title, String description, OffsetDateTime opensAt,
                         OffsetDateTime closesAt, MotionState state, List<Option> options, int eligibleVoterCount,
                         boolean eligibleToVote, Set<UUID> eligibleMembershipIds, UUID selectedOptionId,
                         boolean resultsPublished) {
    public record Option(UUID id, int position, String label) {}
}
