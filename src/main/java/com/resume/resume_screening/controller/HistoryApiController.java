package com.resume.resume_screening.controller;

import org.springframework.security.core.Authentication;
import com.resume.resume_screening.service.PersistenceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HistoryApiController {

    private final PersistenceQueryService persistenceQueryService;

    public HistoryApiController(PersistenceQueryService persistenceQueryService) {
        this.persistenceQueryService = persistenceQueryService;
    }

    @GetMapping("/jobs")
    public List<PersistenceQueryService.JobSummary> getJobs() {
        return persistenceQueryService.getJobs();
    }

    @GetMapping("/resumes")
    public List<PersistenceQueryService.ResumeSummary> getResumes() {
        return persistenceQueryService.getResumes();
    }

    @GetMapping("/rankings")
    public List<PersistenceQueryService.RankingResultSummary> getRankings(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return persistenceQueryService.getRankingHistory();
        }
        return persistenceQueryService.getRankingHistoryForUser(authentication.getName());
    }

    @GetMapping("/jobs/{jobId}/rankings")
    public List<PersistenceQueryService.RankingResultSummary> getRankingsForJob(@PathVariable Long jobId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return persistenceQueryService.getRankingHistoryForJob(jobId);
        }
        return persistenceQueryService.getRankingHistoryForUserAndJob(authentication.getName(), jobId);
    }

    @GetMapping("/my-results")
    public List<PersistenceQueryService.RankingResultSummary> myResults(Authentication authentication) {
        return persistenceQueryService.getRankingHistoryForUser(authentication.getName());
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
