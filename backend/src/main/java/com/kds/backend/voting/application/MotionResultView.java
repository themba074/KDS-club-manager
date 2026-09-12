package com.kds.backend.voting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MotionResultView(UUID motionId, boolean published, OffsetDateTime publishedAt, int totalVotes,
                               int eligibleVoterCount, Outcome outcome, UUID winningOptionId,
                               List<OptionResult> options) {
    public enum Outcome { WINNER, NO_MAJORITY }
    public record OptionResult(UUID optionId, int position, String label, int voteCount) {}
}
