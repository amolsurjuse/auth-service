package com.electrahub.identity.repository;

import com.electrahub.identity.domain.VerificationChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, UUID> {
    Optional<VerificationChallenge> findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
            UUID userId,
            String purpose,
            VerificationChallenge.Status status
    );

    List<VerificationChallenge> findAllByUserIdAndPurposeAndStatus(
            UUID userId,
            String purpose,
            VerificationChallenge.Status status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from VerificationChallenge c where c.id = :id")
    Optional<VerificationChallenge> findByIdForUpdate(@Param("id") UUID id);
}
