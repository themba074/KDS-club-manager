CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    previous_value VARCHAR(80),
    new_value VARCHAR(80),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX audit_log_club_time ON audit_log(club_id, occurred_at DESC, id DESC);
CREATE INDEX audit_log_club_actor_time ON audit_log(club_id, actor_id, occurred_at DESC);
CREATE INDEX audit_log_club_action_time ON audit_log(club_id, action, occurred_at DESC);

-- Feature 16's viewer is administrator-only; existing Chairperson grants predate it.
DELETE FROM role_permissions WHERE role_code = 'CHAIRPERSON' AND permission_code = 'AUDIT_READ';
