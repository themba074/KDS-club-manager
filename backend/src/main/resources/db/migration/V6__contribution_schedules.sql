CREATE TABLE contribution_schedules (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE contribution_schedule_versions (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES contribution_schedules(id),
    club_id UUID NOT NULL REFERENCES clubs(id),
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    name VARCHAR(120) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('MONTHLY', 'ONCE_OFF')),
    first_due_date DATE NOT NULL,
    end_date DATE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    assignment_mode VARCHAR(20) NOT NULL CHECK (assignment_mode IN ('ALL_CURRENT', 'SELECTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT contribution_schedule_version_unique UNIQUE (schedule_id, version_number),
    CONSTRAINT contribution_schedule_dates_valid CHECK (end_date IS NULL OR end_date >= first_due_date),
    CONSTRAINT contribution_schedule_effective_dates_valid CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE contribution_schedule_assignments (
    schedule_version_id UUID NOT NULL REFERENCES contribution_schedule_versions(id),
    club_id UUID NOT NULL REFERENCES clubs(id),
    membership_id UUID NOT NULL REFERENCES club_memberships(id),
    PRIMARY KEY (schedule_version_id, membership_id)
);

CREATE INDEX contribution_schedules_club ON contribution_schedules(club_id, created_at, id);
CREATE INDEX contribution_schedule_versions_club ON contribution_schedule_versions(club_id, schedule_id, version_number);
CREATE INDEX contribution_schedule_assignments_club ON contribution_schedule_assignments(club_id, membership_id);
