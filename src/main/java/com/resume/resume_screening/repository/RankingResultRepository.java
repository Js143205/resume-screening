package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.RankingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankingResultRepository extends JpaRepository<RankingResult, Long> {
    List<RankingResult> findAllByOrderByCreatedAtDesc();
    List<RankingResult> findByJobDescriptionIdOrderByScoreDesc(Long jobDescriptionId);
    List<RankingResult> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    List<RankingResult> findByOwnerUsernameAndJobDescriptionIdOrderByScoreDesc(String username, Long jobDescriptionId);
}
