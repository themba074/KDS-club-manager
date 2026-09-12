package com.kds.backend.documents.application;

import com.kds.backend.documents.domain.DocumentEntity;
import com.kds.backend.documents.domain.DocumentVersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel="spring")
public interface DocumentMapper {
    @Mapping(target="visibleRoleCodes", expression="java(document.getVisibleRoleCodes())")
    @Mapping(target="versions", expression="java(document.getVersions().stream().map(this::view).toList())")
    DocumentView view(DocumentEntity document);
    DocumentVersionView view(DocumentVersionEntity version);
    default OffsetDateTime map(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
}
