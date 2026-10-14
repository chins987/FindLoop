package com.findloop.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MatchFeedbackRepository extends JpaRepository<MatchFeedback, Long> {
    boolean existsByViewerIdAndSourceIdAndCandidateId(String viewerId, Long sourceId, Long candidateId);
    @Query("select case when f.sourceId = :source then f.candidateId else f.sourceId end from MatchFeedback f where f.viewerId = :viewer and (f.sourceId = :source or f.candidateId = :source)")
    List<Long> findCandidateIdsByViewerIdAndSourceId(@Param("viewer") String viewerId, @Param("source") Long sourceId);
}
