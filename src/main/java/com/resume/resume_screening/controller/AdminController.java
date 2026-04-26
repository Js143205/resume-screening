package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    public AdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        AdminDashboardService.AdminDashboardData dashboardData = adminDashboardService.getDashboardData();
        model.addAttribute("dashboard", dashboardData);
        return "admin";
    }

    @GetMapping("/admin/result/{id}")
    @ResponseBody
    public AdminDashboardService.RankingResultDetails rankingResultDetails(@PathVariable Long id) {
        return adminDashboardService.getRankingResultDetails(id);
    }

    @DeleteMapping("/admin/result/{id}")
    @ResponseBody
    public ResponseEntity<AdminDashboardService.DeleteResultResponse> deleteRankingResult(@PathVariable Long id) {
        return ResponseEntity.ok(adminDashboardService.deleteRankingResult(id));
    }
}
