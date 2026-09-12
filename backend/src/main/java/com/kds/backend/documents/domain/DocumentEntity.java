package com.kds.backend.documents.domain;

import com.kds.backend.documents.application.StoredFile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "club_documents")
public class DocumentEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 80) private String category;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id", nullable = false)
    private Set<DocumentVisibilityRoleEntity> visibilityRoles = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id", nullable = false)
    private Set<DocumentVersionEntity> versions = new LinkedHashSet<>();

    protected DocumentEntity() {}

    public DocumentEntity(UUID id, UUID clubId, UUID actor, String title, String category,
                          Set<String> visibleRoleCodes, Instant now) {
        this.id = id;
        this.clubId = clubId;
        this.createdBy = actor;
        this.createdAt = now;
        updateMetadata(title, category, visibleRoleCodes, now);
    }

    public void updateMetadata(String title, String category, Set<String> visibleRoleCodes, Instant now) {
        this.title = title;
        this.category = category;
        this.updatedAt = now;
        visibilityRoles.removeIf(existing -> !visibleRoleCodes.contains(existing.getRoleCode()));
        Set<String> retained = getVisibleRoleCodes();
        visibleRoleCodes.stream().filter(role -> !retained.contains(role))
                .forEach(role -> visibilityRoles.add(new DocumentVisibilityRoleEntity(id, clubId, role)));
    }

    public DocumentVersionEntity addVersion(UUID versionId, UUID actor, Instant now, StoredFile file) {
        DocumentVersionEntity added = new DocumentVersionEntity(versionId, id, clubId,
                versions.size() + 1, actor, now, file);
        versions.add(added);
        updatedAt = now;
        return added;
    }

    public UUID getId() { return id; }
    public UUID getClubId() { return clubId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public Set<String> getVisibleRoleCodes() {
        return visibilityRoles.stream().map(DocumentVisibilityRoleEntity::getRoleCode)
                .collect(Collectors.toUnmodifiableSet());
    }
    public List<DocumentVersionEntity> getVersions() {
        return versions.stream().sorted(Comparator.comparingInt(DocumentVersionEntity::getVersionNumber).reversed())
                .toList();
    }
    public DocumentVersionEntity requireVersion(UUID versionId) {
        return versions.stream().filter(candidate -> candidate.getId().equals(versionId)).findFirst()
                .orElse(null);
    }
}
