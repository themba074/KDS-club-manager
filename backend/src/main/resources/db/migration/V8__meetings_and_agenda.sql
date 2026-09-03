CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    utc_offset_minutes INTEGER NOT NULL CHECK (utc_offset_minutes BETWEEN -1080 AND 1080),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 15 AND 1440),
    location VARCHAR(240),
    meeting_url VARCHAR(500),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT meetings_place_required CHECK (
        (location IS NOT NULL AND TRIM(location) <> '') OR
        (meeting_url IS NOT NULL AND TRIM(meeting_url) <> '')
    )
);

CREATE TABLE meeting_agenda_items (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    club_id UUID NOT NULL REFERENCES clubs(id),
    position INTEGER NOT NULL CHECK (position >= 0),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    CONSTRAINT meeting_agenda_position_unique UNIQUE (meeting_id, position)
);

CREATE INDEX meetings_club_start ON meetings(club_id, starts_at, id);
CREATE INDEX meeting_agenda_club_meeting ON meeting_agenda_items(club_id, meeting_id, position);
