package com.kds.backend.voting.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VoteReceipt(UUID motionId, UUID optionId, String optionLabel, OffsetDateTime castAt) {}
