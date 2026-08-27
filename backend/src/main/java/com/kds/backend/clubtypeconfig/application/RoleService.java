package com.kds.backend.clubtypeconfig.application;
import com.kds.backend.clubtypeconfig.domain.RoleEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
/** Public application boundary for global club-type configuration, never tenant assignments. */
@Service @Transactional(readOnly = true)
public class RoleService {
    private final EntityManager entityManager;
    private final RoleMapper mapper;
    public RoleService(EntityManager entityManager, RoleMapper mapper) { this.entityManager = entityManager; this.mapper = mapper; }
    public List<RoleDefinition> roles(String clubType) {
        return entityManager.createQuery("select distinct r from RoleEntity r left join fetch r.permissions where r.clubType = :type order by r.name", RoleEntity.class)
            .setParameter("type", clubType).getResultList().stream().map(mapper::definition).toList();
    }
    public RoleDefinition requireRole(String clubType, String code) {
        return roles(clubType).stream().filter(role -> role.code().equals(code)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role for this club type."));
    }
}

