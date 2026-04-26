package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.RecruiterDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RecruiterController.class)
class RecruiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecruiterDashboardService recruiterDashboardService;

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void recruiterDashboardRendersWithMetrics() throws Exception {
        RecruiterDashboardService.RecruiterDashboardData dashboardData = new RecruiterDashboardService.RecruiterDashboardData(
                12,
                5,
                76.4,
                List.of(new RecruiterDashboardService.RecentRankingRow(
                        101L,
                        "Java Backend Engineer",
                        "resume1.pdf",
                        82,
                        LocalDateTime.of(2026, 4, 16, 10, 30)
                )),
                List.of(new RecruiterDashboardService.SkillFrequency("java", 8)),
                8,
                new RecruiterDashboardService.ScoreDistribution(1, 2, 3, 4)
        );

        given(recruiterDashboardService.getDashboardData("recruiter")).willReturn(dashboardData);

        mockMvc.perform(get("/recruiter"))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter"))
                .andExpect(model().attributeExists("analytics"))
                .andExpect(model().attribute("resultApiBasePath", "/recruiter/result"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Screening Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java Backend Engineer")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("resume1.pdf")));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void recruiterResultEndpointsReturnDetailsAndAllowDelete() throws Exception {
        given(recruiterDashboardService.getRankingResultDetails("recruiter", 101L))
                .willReturn(new RecruiterDashboardService.RankingResultDetails(
                        101L,
                        "Java Backend Engineer",
                        "resume1.pdf",
                        82,
                        78,
                        84,
                        83,
                        List.of("java", "spring"),
                        "Good profile.",
                        LocalDateTime.of(2026, 4, 16, 10, 30)
                ));
        given(recruiterDashboardService.deleteRankingResult("recruiter", 101L))
                .willReturn(new RecruiterDashboardService.DeleteResultResponse(101L, "Ranking result deleted successfully."));

        mockMvc.perform(get("/recruiter/result/101"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"id\":101")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resumeFileName\":\"resume1.pdf\"")));

        mockMvc.perform(delete("/recruiter/result/101").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ranking result deleted successfully.")));
    }
}
