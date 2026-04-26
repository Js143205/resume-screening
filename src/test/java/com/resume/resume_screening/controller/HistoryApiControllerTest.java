package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.PersistenceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryApiController.class)
class HistoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersistenceQueryService persistenceQueryService;

    @Test
    void getJobsReturnsStoredJobs() throws Exception {
        given(persistenceQueryService.getJobs()).willReturn(List.of(
                new PersistenceQueryService.JobSummary(1L, "Backend Developer", "Spring Boot + MySQL", LocalDateTime.of(2026, 3, 26, 1, 0))
        ));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Backend Developer"))
                .andExpect(jsonPath("$[0].description").value("Spring Boot + MySQL"));
    }
}
