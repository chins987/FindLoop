package com.findloop.item;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OwnershipVerificationRepository extends JpaRepository<OwnershipVerification, Long> {
    Optional<OwnershipVerification> findByReportIdAndUserId(Long reportId, String userId);
    boolean existsByReportIdAndUserIdAndVerifiedAtAfter(Long reportId, String userId, LocalDateTime after);
}
