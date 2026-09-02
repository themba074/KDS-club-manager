CREATE TABLE contribution_payments (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    schedule_version_id UUID NOT NULL REFERENCES contribution_schedule_versions(id),
    membership_id UUID NOT NULL REFERENCES club_memberships(id),
    due_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    received_on DATE NOT NULL,
    reference VARCHAR(120),
    note VARCHAR(500),
    proof_storage_key VARCHAR(500),
    proof_file_name VARCHAR(255),
    proof_content_type VARCHAR(120),
    recorded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX contribution_payments_club_member_due
    ON contribution_payments(club_id, membership_id, due_date, created_at);
CREATE INDEX contribution_payments_club_schedule_due
    ON contribution_payments(club_id, schedule_version_id, due_date);
