CREATE TABLE club_documents (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_document_tenant_scope UNIQUE (id, club_id)
);

CREATE TABLE document_visibility_roles (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    document_id UUID NOT NULL,
    role_code VARCHAR(80) NOT NULL,
    CONSTRAINT uq_document_visibility_role UNIQUE (document_id, role_code),
    CONSTRAINT fk_document_visibility_tenant FOREIGN KEY (document_id, club_id)
        REFERENCES club_documents(id, club_id)
);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    storage_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_document_version_number UNIQUE (document_id, version_number),
    CONSTRAINT uq_document_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_document_version_tenant_scope UNIQUE (id, document_id, club_id),
    CONSTRAINT fk_document_version_tenant FOREIGN KEY (document_id, club_id)
        REFERENCES club_documents(id, club_id)
);

CREATE INDEX idx_documents_club_updated ON club_documents(club_id, updated_at DESC);
CREATE INDEX idx_document_visibility_club_role ON document_visibility_roles(club_id, role_code);
CREATE INDEX idx_document_versions_club_document ON document_versions(club_id, document_id, version_number DESC);
