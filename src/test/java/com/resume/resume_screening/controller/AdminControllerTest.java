package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @Test
    void adminDashboardRendersWithMetrics() throws Exception {
        AdminDashboardService.AdminDashboardData dashboardData = new AdminDashboardService.AdminDashboardData(
                12,
                5,
                76.4,
                List.of(new AdminDashboardService.RecentRankingRow(
                        101L,
                        "Java Backend Engineer",
                        "resume1.pdf",
                        82,
                        LocalDateTime.of(2026, 4, 16, 10, 30)
                )),
                List.of(new AdminDashboardService.SkillFrequency("java", 8)),
                8,
                new AdminDashboardService.ScoreDistribution(1, 2, 3, 4)
        );

        given(adminDashboardService.getDashboardData()).willReturn(dashboardData);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Screening Admin Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java Backend Engineer")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("resume1.pdf")));
    }
}
