CREATE TABLE meeting_rsvps (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES club_memberships(id),
    response VARCHAR(10) NOT NULL CHECK (response IN ('YES', 'NO', 'MAYBE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_meeting_rsvp_member UNIQUE (meeting_id, membership_id)
);

CREATE INDEX idx_meeting_rsvps_club_meeting ON meeting_rsvps(club_id, meeting_id);

CREATE TABLE meeting_minutes (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    body VARCHAR(20000),
    attachment_key VARCHAR(1000),
    attachment_name VARCHAR(255),
    attachment_content_type VARCHAR(120),
    attachment_size BIGINT,
    published_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_meeting_minutes_meeting UNIQUE (meeting_id),
    CONSTRAINT ck_meeting_minutes_content CHECK (body IS NOT NULL OR attachment_key IS NOT NULL)
);

CREATE INDEX idx_meeting_minutes_club_meeting ON meeting_minutes(club_id, meeting_id);
