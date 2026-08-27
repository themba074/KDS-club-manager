package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.ClubMembershipEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClubMapper {
    @Mapping(target = "id", source = "club.id")
    @Mapping(target = "name", source = "club.name")
    @Mapping(target = "clubType", source = "club.clubType")
    @Mapping(target = "permissions", expression = "java(java.util.List.of())")
    ClubSummary summary(ClubMembershipEntity membership);
}
