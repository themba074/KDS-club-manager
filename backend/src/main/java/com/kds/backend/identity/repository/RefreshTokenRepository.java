package com.kds.backend.identity.repository;

import com.kds.backend.identity.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    @org.springframework.data.jpa.repository.Query("select t.user.id from RefreshTokenEntity t where t.tokenHash = :tokenHash")
    Optional<UUID> ownerOf(String tokenHash);
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenEntity> findAllByFamilyId(UUID familyId);
    long deleteByUserId(UUID userId);
}
