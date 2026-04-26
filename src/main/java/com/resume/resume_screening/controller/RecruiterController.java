package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.RecruiterDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RecruiterController {

    private final RecruiterDashboardService recruiterDashboardService;

    public RecruiterController(RecruiterDashboardService recruiterDashboardService) {
        this.recruiterDashboardService = recruiterDashboardService;
    }

    @GetMapping("/recruiter")
    public String recruiterDashboard(Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        RecruiterDashboardService.RecruiterDashboardData dashboardData = recruiterDashboardService.getDashboardData(username);
        model.addAttribute("activePage", "recruiter");
        model.addAttribute("analytics", dashboardData);
        model.addAttribute("resultApiBasePath", "/recruiter/result");
        return "recruiter";
    }

    @GetMapping("/recruiter/result/{id}")
    @ResponseBody
    public RecruiterDashboardService.RankingResultDetails rankingResultDetails(@PathVariable Long id,
                                                                               Authentication authentication) {
        return recruiterDashboardService.getRankingResultDetails(authentication != null ? authentication.getName() : null, id);
    }

    @DeleteMapping("/recruiter/result/{id}")
    @ResponseBody
    public ResponseEntity<RecruiterDashboardService.DeleteResultResponse> deleteRankingResult(@PathVariable Long id,
                                                                                               Authentication authentication) {
        return ResponseEntity.ok(recruiterDashboardService.deleteRankingResult(authentication != null ? authentication.getName() : null, id));
    }
}
