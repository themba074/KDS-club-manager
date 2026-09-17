CREATE TABLE club_type_configs (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    administrator_role_code VARCHAR(40) NOT NULL REFERENCES roles(code),
    default_member_role_code VARCHAR(40) NOT NULL REFERENCES roles(code),
    member_label VARCHAR(60) NOT NULL,
    contribution_label VARCHAR(60) NOT NULL
);
CREATE TABLE club_type_modules (
    club_type_code VARCHAR(40) NOT NULL REFERENCES club_type_configs(code),
    module_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (club_type_code, module_code)
);

INSERT INTO roles(code, name, club_type) VALUES
    ('SPORTS_ADMINISTRATOR', 'Administrator', 'SPORTS_CLUB'),
    ('SPORTS_MEMBER', 'Member', 'SPORTS_CLUB');
INSERT INTO role_permissions(role_code, permission_code)
SELECT 'SPORTS_ADMINISTRATOR', code FROM permissions WHERE code IN
    ('ROLES_READ', 'ROLES_MANAGE', 'MEMBERS_READ', 'MEMBERS_WRITE', 'MEETINGS_READ', 'MEETINGS_WRITE', 'DOCUMENTS_READ', 'DOCUMENTS_MANAGE');
INSERT INTO role_permissions(role_code, permission_code)
SELECT 'SPORTS_MEMBER', code FROM permissions WHERE code IN
    ('MEMBERS_READ', 'MEETINGS_READ', 'DOCUMENTS_READ');

INSERT INTO club_type_configs VALUES
    ('INVESTMENT_CLUB', 'Investment Club', 'ADMINISTRATOR', 'MEMBER', 'Members', 'Contributions'),
    ('SPORTS_CLUB', 'Sports Club', 'SPORTS_ADMINISTRATOR', 'SPORTS_MEMBER', 'Players', 'Dues');
INSERT INTO club_type_modules(club_type_code, module_code)
SELECT 'INVESTMENT_CLUB', module_code FROM (VALUES
    ('ROLES'), ('MEMBERS'), ('CONTRIBUTIONS'), ('MEETINGS'), ('VOTING'),
    ('DOCUMENTS'), ('NOTIFICATIONS'), ('AUDIT'), ('REPORTS')) AS modules(module_code);
INSERT INTO club_type_modules(club_type_code, module_code)
SELECT 'SPORTS_CLUB', module_code FROM (VALUES
    ('ROLES'), ('MEMBERS'), ('MEETINGS'), ('DOCUMENTS'), ('NOTIFICATIONS')) AS modules(module_code);

ALTER TABLE clubs DROP CONSTRAINT IF EXISTS clubs_club_type_check;
ALTER TABLE clubs ADD CONSTRAINT clubs_club_type_fkey FOREIGN KEY (club_type) REFERENCES club_type_configs(code);
