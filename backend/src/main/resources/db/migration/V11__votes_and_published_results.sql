ALTER TABLE motions ADD COLUMN results_published_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE motions ADD COLUMN results_published_by UUID;
ALTER TABLE motions ADD COLUMN result_outcome VARCHAR(30);
ALTER TABLE motions ADD COLUMN winning_option_id UUID;
ALTER TABLE motions ADD COLUMN total_votes INTEGER;

ALTER TABLE motions
    ADD CONSTRAINT ck_motion_result_publication CHECK (
        (results_published_at IS NULL AND results_published_by IS NULL AND result_outcome IS NULL
            AND winning_option_id IS NULL AND total_votes IS NULL)
        OR
        (results_published_at IS NOT NULL AND results_published_by IS NOT NULL AND result_outcome IS NOT NULL
            AND total_votes IS NOT NULL AND total_votes >= 0)
    );
ALTER TABLE motions
    ADD CONSTRAINT ck_motion_result_outcome CHECK (result_outcome IS NULL OR result_outcome IN ('WINNER', 'NO_MAJORITY'));
ALTER TABLE motions
    ADD CONSTRAINT ck_motion_winner CHECK (
        (result_outcome = 'WINNER' AND winning_option_id IS NOT NULL)
        OR (result_outcome IS NULL)
        OR (result_outcome = 'NO_MAJORITY' AND winning_option_id IS NULL)
    );
ALTER TABLE motions ADD CONSTRAINT uq_motion_tenant_scope UNIQUE (id, club_id);

ALTER TABLE motion_options
    ADD CONSTRAINT uq_motion_option_tenant_scope UNIQUE (id, motion_id, club_id);

ALTER TABLE club_memberships
    ADD CONSTRAINT uq_club_membership_tenant_scope UNIQUE (id, club_id);

CREATE TABLE votes (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    motion_id UUID NOT NULL,
    option_id UUID NOT NULL,
    membership_id UUID NOT NULL,
    cast_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_vote_motion_membership UNIQUE (motion_id, membership_id),
    CONSTRAINT fk_vote_motion_tenant FOREIGN KEY (motion_id, club_id) REFERENCES motions(id, club_id),
    CONSTRAINT fk_vote_option_motion_tenant FOREIGN KEY (option_id, motion_id, club_id)
        REFERENCES motion_options(id, motion_id, club_id),
    CONSTRAINT fk_vote_membership_tenant FOREIGN KEY (membership_id, club_id)
        REFERENCES club_memberships(id, club_id)
);
CREATE INDEX idx_votes_club_motion ON votes(club_id, motion_id);

ALTER TABLE motions
    ADD CONSTRAINT fk_motion_winning_option FOREIGN KEY (winning_option_id, id, club_id)
        REFERENCES motion_options(id, motion_id, club_id);

CREATE TABLE motion_result_option_counts (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    motion_id UUID NOT NULL,
    option_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    label VARCHAR(200) NOT NULL,
    vote_count INTEGER NOT NULL CHECK (vote_count >= 0),
    CONSTRAINT uq_motion_result_option UNIQUE (motion_id, option_id),
    CONSTRAINT uq_motion_result_position UNIQUE (motion_id, position),
    CONSTRAINT fk_result_motion_tenant FOREIGN KEY (motion_id, club_id) REFERENCES motions(id, club_id),
    CONSTRAINT fk_result_option_motion_tenant FOREIGN KEY (option_id, motion_id, club_id)
        REFERENCES motion_options(id, motion_id, club_id)
);
CREATE INDEX idx_motion_results_club_motion ON motion_result_option_counts(club_id, motion_id);
