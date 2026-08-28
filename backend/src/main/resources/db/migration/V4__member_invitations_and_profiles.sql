CREATE TABLE member_invitations (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    email VARCHAR(320) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    role_code VARCHAR(40) NOT NULL REFERENCES roles(code),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    invited_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT member_invitations_club_email_unique UNIQUE (club_id, email)
);
CREATE INDEX member_invitations_club_index ON member_invitations(club_id);

CREATE TABLE member_profiles (
    membership_id UUID PRIMARY KEY REFERENCES club_memberships(id),
    club_id UUID NOT NULL REFERENCES clubs(id),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX member_profiles_club_index ON member_profiles(club_id);
