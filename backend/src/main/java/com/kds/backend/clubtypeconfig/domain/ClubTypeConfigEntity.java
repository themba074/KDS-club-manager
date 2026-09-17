package com.kds.backend.clubtypeconfig.domain;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "club_type_configs")
public class ClubTypeConfigEntity {
    @Id private String code;
    @Column(nullable = false) private String name;
    @Column(name = "administrator_role_code", nullable = false) private String administratorRoleCode;
    @Column(name = "default_member_role_code", nullable = false) private String defaultMemberRoleCode;
    @Column(name = "member_label", nullable = false) private String memberLabel;
    @Column(name = "contribution_label", nullable = false) private String contributionLabel;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "club_type_modules", joinColumns = @JoinColumn(name = "club_type_code"))
    @Column(name = "module_code") private Set<String> enabledModules;

    protected ClubTypeConfigEntity() {}
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAdministratorRoleCode() { return administratorRoleCode; }
    public String getDefaultMemberRoleCode() { return defaultMemberRoleCode; }
    public String getMemberLabel() { return memberLabel; }
    public String getContributionLabel() { return contributionLabel; }
    public Set<String> getEnabledModules() { return enabledModules; }
}
