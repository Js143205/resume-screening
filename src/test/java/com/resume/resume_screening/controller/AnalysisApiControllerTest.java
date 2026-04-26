package com.resume.resume_screening.controller;

import com.resume.resume_screening.service.AnalysisResult;
import com.resume.resume_screening.service.ResumeAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisApiController.class)
class AnalysisApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeAnalysisService resumeAnalysisService;

    @Test
    void analyzeReturnsJsonPayload() throws Exception {
        AnalysisResult.ResumeInsight resume1 = new AnalysisResult.ResumeInsight(
                1,
                "resume1.txt",
                "text/plain",
                88,
                new AnalysisResult.ScoreBreakdown(90, 80, 78),
                List.of("java", "spring boot"),
                List.of("java", "spring", "spring boot"),
                "resume text 1",
                "resume 1 explanation",
                "python_nlp"
        );
        AnalysisResult.ResumeInsight resume2 = new AnalysisResult.ResumeInsight(
                2,
                "resume2.txt",
                "text/plain",
                72,
                new AnalysisResult.ScoreBreakdown(70, 68, 66),
                List.of("python"),
                List.of("python", "api"),
                "resume text 2",
                "resume 2 explanation",
                "java_fallback"
        );

        given(resumeAnalysisService.analyze(eq("Java Developer"), any(), eq("Spring Boot role")))
                .willReturn(new AnalysisResult("Java Developer", List.of(resume1, resume2), "resume1.txt is the best match"));

        MockMultipartFile file1 = new MockMultipartFile("files", "resume1.txt", "text/plain", "java spring".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "resume2.txt", "text/plain", "python api".getBytes());

        mockMvc.perform(multipart("/api/analyze")
                        .file(file1)
                        .file(file2)
                        .param("jobTitle", "Java Developer")
                        .param("jobDesc", "Spring Boot role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.best").value("resume1.txt is the best match"))
                .andExpect(jsonPath("$.totalResumes").value(2))
                .andExpect(jsonPath("$.resume1.score").value(88))
                .andExpect(jsonPath("$.rankedResumes[0].fileName").value("resume1.txt"))
                .andExpect(jsonPath("$.rankedResumes[0].breakdown.skillScore").value(90))
                .andExpect(jsonPath("$.rankedResumes[0].matchedSkills[0]").value("java"));
    }
}
