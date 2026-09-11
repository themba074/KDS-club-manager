package com.kds.backend.voting.application;

import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.domain.MotionOptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MotionMapper {
    @Mapping(target = "eligibleMembershipIds", source = "visibleEligibleMembershipIds")
    MotionView view(MotionEntity motion, MotionView.State state, int eligibleVoterCount,
                    boolean eligibleToVote, Set<UUID> visibleEligibleMembershipIds);
    MotionView.Option option(MotionOptionEntity option);
    default OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
