package com.kds.backend.documents.domain;

import com.kds.backend.documents.application.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
public class DocumentVersionEntity {
    @Id private UUID id;
    @Column(name = "club_id", nullable = false) private UUID clubId;
    @Column(name = "document_id", insertable = false, updatable = false) private UUID documentId;
    @Column(name = "version_number", nullable = false) private int versionNumber;
    @Column(name = "storage_key", nullable = false, length = 1000) private String storageKey;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "content_type", nullable = false, length = 150) private String contentType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "uploaded_by", nullable = false) private UUID uploadedBy;
    @Column(name = "uploaded_at", nullable = false) private Instant uploadedAt;

    protected DocumentVersionEntity() {}

    public DocumentVersionEntity(UUID id, UUID documentId, UUID clubId, int versionNumber,
                                 UUID uploadedBy, Instant uploadedAt, StoredFile file) {
        this.id = id;
        this.documentId = documentId;
        this.clubId = clubId;
        this.versionNumber = versionNumber;
        this.storageKey = file.storageKey();
        this.fileName = file.fileName();
        this.contentType = file.contentType();
        this.fileSize = file.size();
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() { return id; }
    public int getVersionNumber() { return versionNumber; }
    public String getStorageKey() { return storageKey; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public UUID getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
}
