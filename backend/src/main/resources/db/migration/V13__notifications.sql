ALTER TABLE club_memberships ADD CONSTRAINT uq_membership_tenant_scope UNIQUE (id, club_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    membership_id UUID NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    target_path VARCHAR(500),
    source_type VARCHAR(40) NOT NULL,
    source_id UUID NOT NULL,
    deduplication_key VARCHAR(500) NOT NULL,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    email_attempts INTEGER NOT NULL DEFAULT 0 CHECK (email_attempts >= 0),
    email_attempted_at TIMESTAMP WITH TIME ZONE,
    emailed_at TIMESTAMP WITH TIME ZONE,
    email_error VARCHAR(1000),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_notification_deduplication UNIQUE (club_id, deduplication_key),
    CONSTRAINT fk_notification_membership_tenant FOREIGN KEY (membership_id, club_id)
        REFERENCES club_memberships(id, club_id)
);
CREATE INDEX idx_notifications_feed ON notifications(club_id,membership_id,available_at DESC);
CREATE INDEX idx_notifications_delivery ON notifications(club_id,available_at,emailed_at,cancelled_at);
