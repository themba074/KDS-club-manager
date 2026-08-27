package com.kds.backend.identity.repository;

import com.kds.backend.identity.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> lockById(UUID id);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
