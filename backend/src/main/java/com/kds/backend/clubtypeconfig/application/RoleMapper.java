package com.kds.backend.clubtypeconfig.application;
import com.kds.backend.clubtypeconfig.domain.RoleEntity;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface RoleMapper { RoleDefinition definition(RoleEntity entity); }

