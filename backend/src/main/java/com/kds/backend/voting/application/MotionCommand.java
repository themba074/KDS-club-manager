package com.kds.backend.voting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MotionCommand(long version, String title, String description, OffsetDateTime opensAt,
                            OffsetDateTime closesAt, List<String> options, Set<UUID> eligibleMembershipIds,
                            boolean allActiveMembers) {}
