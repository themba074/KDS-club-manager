CREATE TABLE clubs (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    club_type VARCHAR(40) NOT NULL CHECK (club_type = 'INVESTMENT_CLUB'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE club_memberships (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    user_id UUID NOT NULL REFERENCES users(id),
    administrator BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT club_memberships_unique UNIQUE (club_id, user_id)
);
CREATE INDEX club_memberships_user_index ON club_memberships(user_id);

ALTER TABLE refresh_tokens ADD COLUMN active_club_id UUID REFERENCES clubs(id);
