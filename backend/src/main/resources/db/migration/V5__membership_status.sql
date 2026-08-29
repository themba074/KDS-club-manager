ALTER TABLE club_memberships
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE club_memberships
    ADD CONSTRAINT club_memberships_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXITED'));
CREATE INDEX club_memberships_club_status_index ON club_memberships(club_id, status);
