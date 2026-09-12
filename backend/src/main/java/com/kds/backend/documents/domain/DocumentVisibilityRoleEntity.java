package com.kds.backend.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "document_visibility_roles")
public class DocumentVisibilityRoleEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "document_id", insertable = false, updatable = false) private UUID documentId;
    @Column(name = "role_code", nullable = false, length = 80) private String roleCode;

    protected DocumentVisibilityRoleEntity() {}

    public DocumentVisibilityRoleEntity(UUID documentId, UUID clubId, String roleCode) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.clubId = clubId;
        this.roleCode = roleCode;
    }

    public String getRoleCode() { return roleCode; }
}
