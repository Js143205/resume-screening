package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.PersistenceQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminResultsApiController {

    private final PersistenceQueryService persistenceQueryService;

    public AdminResultsApiController(PersistenceQueryService persistenceQueryService) {
        this.persistenceQueryService = persistenceQueryService;
    }

    @GetMapping("/results")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PersistenceQueryService.RankingResultSummary> allResults() {
        return persistenceQueryService.getRankingHistory();
    }
}

