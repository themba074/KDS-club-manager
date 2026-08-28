package com.kds.backend.identity.repository;

import com.kds.backend.identity.domain.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    @org.springframework.data.jpa.repository.Query("select t.user.id from PasswordResetTokenEntity t where t.tokenHash = :tokenHash")
    Optional<UUID> ownerOf(String tokenHash);
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
    long deleteByUserId(UUID userId);
}
