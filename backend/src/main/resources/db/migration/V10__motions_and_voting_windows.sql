CREATE TABLE motions (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_motion_window CHECK (closes_at > opens_at)
);
CREATE INDEX idx_motions_club_window ON motions(club_id, opens_at, closes_at);
CREATE TABLE motion_options (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    motion_id UUID NOT NULL REFERENCES motions(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    label VARCHAR(200) NOT NULL,
    CONSTRAINT uq_motion_option_position UNIQUE (motion_id, position)
);
CREATE TABLE motion_eligible_memberships (
    id UUID PRIMARY KEY,
    motion_id UUID NOT NULL REFERENCES motions(id) ON DELETE CASCADE,
    club_id UUID NOT NULL REFERENCES clubs(id),
    membership_id UUID NOT NULL REFERENCES club_memberships(id),
    CONSTRAINT uq_motion_eligible_membership UNIQUE (motion_id, membership_id)
);
CREATE INDEX idx_motion_eligible_memberships_club_member ON motion_eligible_memberships(club_id, membership_id);
