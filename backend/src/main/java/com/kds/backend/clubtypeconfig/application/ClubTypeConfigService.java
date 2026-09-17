package com.kds.backend.clubtypeconfig.application;

import com.kds.backend.clubtypeconfig.domain.ClubTypeConfigEntity;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

/** Global template catalog. Tenant-specific role assignments remain in Identity. */
@Service
@Transactional(readOnly = true)
public class ClubTypeConfigService {
    private final EntityManager entityManager;
    public ClubTypeConfigService(EntityManager entityManager) { this.entityManager = entityManager; }

    public List<ClubTypeConfig> list() {
        return entityManager.createQuery("select c from ClubTypeConfigEntity c order by c.name", ClubTypeConfigEntity.class)
                .getResultList().stream().map(this::definition).toList();
    }

    public ClubTypeConfig require(String code) {
        ClubTypeConfigEntity entity = entityManager.find(ClubTypeConfigEntity.class, code);
        if (entity == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown club type.");
        return definition(entity);
    }

    private ClubTypeConfig definition(ClubTypeConfigEntity entity) {
        return new ClubTypeConfig(entity.getCode(), entity.getName(), entity.getAdministratorRoleCode(),
                entity.getDefaultMemberRoleCode(), entity.getMemberLabel(), entity.getContributionLabel(),
                Set.copyOf(entity.getEnabledModules()));
    }
}
