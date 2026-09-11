package com.kds.backend.voting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MotionView(UUID id, long version, String title, String description, OffsetDateTime opensAt,
                         OffsetDateTime closesAt, State state, List<Option> options, int eligibleVoterCount,
                         boolean eligibleToVote, Set<UUID> eligibleMembershipIds) {
    public enum State { DRAFT, OPEN, CLOSED, CANCELLED }
    public record Option(UUID id, int position, String label) {}
}
