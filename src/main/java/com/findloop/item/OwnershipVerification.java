package com.findloop.item;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ownership_verifications", uniqueConstraints = @UniqueConstraint(columnNames = {"report_id", "user_id"}))
public class OwnershipVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "report_id", nullable = false) private Long reportId;
    @Column(name = "user_id", nullable = false, length = 255) private String userId;
    @Column(nullable = false) private int attempts;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastAttemptAt;
    protected OwnershipVerification() {}
    public OwnershipVerification(Long reportId, String userId) { this.reportId = reportId; this.userId = userId; }
    public int getAttempts() { return attempts; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
}
