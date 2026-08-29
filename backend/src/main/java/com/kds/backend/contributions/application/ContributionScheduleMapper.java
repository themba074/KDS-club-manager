package com.kds.backend.contributions.application;
import com.kds.backend.contributions.domain.ContributionScheduleVersionEntity;
import com.kds.backend.members.application.ContributionMember;
import org.mapstruct.*;
import java.util.List;
@Mapper(componentModel="spring")
public interface ContributionScheduleMapper {
    @Mapping(target="versionId",source="version.id")
    @Mapping(target="assignedMembers",source="members")
    ContributionScheduleView view(ContributionScheduleVersionEntity version,List<ContributionMember> members);
}
