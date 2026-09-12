package com.kds.backend.voting.application;

import com.kds.backend.voting.domain.MotionEntity;
import com.kds.backend.voting.domain.MotionOptionEntity;
import com.kds.backend.voting.domain.MotionState;
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
    @Mapping(target = "resultsPublished", expression = "java(motion.getResultsPublishedAt() != null)")
    MotionView view(MotionEntity motion, MotionState state, int eligibleVoterCount,
                    boolean eligibleToVote, Set<UUID> visibleEligibleMembershipIds, UUID selectedOptionId);
    MotionView.Option option(MotionOptionEntity option);
    default OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
