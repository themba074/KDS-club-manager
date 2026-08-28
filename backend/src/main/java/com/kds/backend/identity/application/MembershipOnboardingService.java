package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.ClubEntity;
import com.kds.backend.identity.domain.ClubMembershipEntity;
import com.kds.backend.identity.domain.UserEntity;
import com.kds.backend.identity.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/** Public Identity application service used by Members after an invitation has been validated. */
@Service
public class MembershipOnboardingService {
    private final UserRepository users;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public MembershipOnboardingService(UserRepository users, EntityManager entityManager,
                                       PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public MembershipOnboardingResult accept(String email, String password, UUID clubId, String roleCode) {
        UserEntity user = users.findByEmail(email).orElse(null);
        boolean accountCreated = user == null;
        if (accountCreated) {
            if (password == null || password.length() < 8 || password.length() > 72) {
                throw new ResponseStatusException(BAD_REQUEST, "Choose a password between 8 and 72 characters.");
            }
            user = users.save(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode(password), clock.instant()));
        }
        boolean alreadyMember = !entityManager.createQuery("select m.id from ClubMembershipEntity m where m.club.id = :clubId and m.userId = :userId", UUID.class)
                .setParameter("clubId", clubId).setParameter("userId", user.getId()).setMaxResults(1).getResultList().isEmpty();
        if (alreadyMember) throw new ResponseStatusException(CONFLICT, "This account already belongs to the club.");

        UUID membershipId = UUID.randomUUID();
        ClubMembershipEntity membership = new ClubMembershipEntity(membershipId,
                entityManager.getReference(ClubEntity.class, clubId), user.getId(), false, clock.instant());
        membership.assignRole(roleCode);
        entityManager.persist(membership);
        return new MembershipOnboardingResult(user.getId(), membershipId, accountCreated);
    }

    public boolean accountExists(String email) { return users.existsByEmail(email); }
}
