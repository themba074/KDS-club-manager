package com.kds.backend.clubtypeconfig.domain;
import com.kds.backend.clubtypeconfig.application.Permission;
import jakarta.persistence.*;
import java.util.Set;
@Entity @Table(name = "roles")
public class RoleEntity {
    @Id private String code;
    private String name;
    @Column(name = "club_type") private String clubType;
    @ElementCollection
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_code"))
    @Column(name = "permission_code") @Enumerated(EnumType.STRING)
    private Set<Permission> permissions;
    protected RoleEntity() {}
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getClubType() { return clubType; }
    public Set<Permission> getPermissions() { return permissions; }
}

