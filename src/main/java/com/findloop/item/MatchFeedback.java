package com.findloop.item;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"viewer_id", "source_id", "candidate_id"}))
public class MatchFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "viewer_id", nullable = false, length = 255) private String viewerId;
    @Column(name = "source_id", nullable = false) private Long sourceId;
    @Column(name = "candidate_id", nullable = false) private Long candidateId;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    protected MatchFeedback() {}
    public MatchFeedback(String viewerId, Long sourceId, Long candidateId) {
        this.viewerId = viewerId; this.sourceId = sourceId; this.candidateId = candidateId;
    }
}
