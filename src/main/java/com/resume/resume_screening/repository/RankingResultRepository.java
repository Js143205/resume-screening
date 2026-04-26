package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.RankingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RankingResultRepository extends JpaRepository<RankingResult, Long> {
    List<RankingResult> findAllByOrderByCreatedAtDesc();

    List<RankingResult> findByJobDescriptionIdOrderByScoreDesc(Long jobDescriptionId);

    // Corrected methods using property navigation (owner.username)
    List<RankingResult> findByOwner_UsernameOrderByCreatedAtDesc(String username);
    List<RankingResult> findByOwner_UsernameAndJobDescriptionIdOrderByScoreDesc(String username, Long jobDescriptionId);
    Optional<RankingResult> findByIdAndOwner_Username(Long id, String username);
}
