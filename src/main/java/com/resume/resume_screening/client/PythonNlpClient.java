package com.resume.resume_screening.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PythonNlpClient {

    private static final Logger log = LoggerFactory.getLogger(PythonNlpClient.class);

    private final RestClient restClient;
    private final boolean enabled;

    public PythonNlpClient(@Value("${nlp.python.base-url:http://localhost:8000}") String baseUrl,
                           @Value("${nlp.python.enabled:true}") boolean enabled) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.enabled = enabled;
    }

    public PythonNlpResult analyze(String resumeText, String jobDescription) {
        if (!enabled) {
            return PythonNlpResult.disabled("Python NLP integration is disabled by configuration.");
        }

        try {
            PythonNlpResponse response = restClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new PythonNlpRequest(resumeText, jobDescription))
                    .retrieve()
                    .body(PythonNlpResponse.class);

            if (response == null) {
                log.warn("Python NLP returned an empty response body.");
                return PythonNlpResult.failed("Python NLP returned an empty response body.");
            }

            return PythonNlpResult.success(response);
        } catch (Exception exception) {
            log.warn("Python NLP request failed: {}", exception.getMessage(), exception);
            return PythonNlpResult.failed("Python NLP request failed: " + exception.getMessage());
        }
    }

    public record PythonNlpRequest(String resumeText, String jobDescription) {
    }

    public static final class PythonNlpResult {
        private final PythonNlpResponse response;
        private final String status;
        private final String message;

        public PythonNlpResult(PythonNlpResponse response, String status, String message) {
            this.response = response;
            this.status = status;
            this.message = message;
        }

        public static PythonNlpResult success(PythonNlpResponse response) {
            return new PythonNlpResult(response, "success", "Python NLP response received.");
        }

        public static PythonNlpResult failed(String message) {
            return new PythonNlpResult(null, "failed", message);
        }

        public static PythonNlpResult disabled(String message) {
            return new PythonNlpResult(null, "disabled", message);
        }

        public PythonNlpResponse response() {
            return response;
        }

        public String status() {
            return status;
        }

        public String message() {
            return message;
        }

        public boolean hasResponse() {
            return response != null;
        }
    }

    public static class PythonNlpResponse {
        private Double finalScore;
        private Double skillScore;
        private Double keywordScore;
        private Double semanticScore;
        private List<String> matchedSkills;
        private List<String> detectedSkills;
        private String explanation;
        private String analysisSource;

        public Double getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(Double finalScore) {
            this.finalScore = finalScore;
        }

        public Double getSkillScore() {
            return skillScore;
        }

        public void setSkillScore(Double skillScore) {
            this.skillScore = skillScore;
        }

        public Double getKeywordScore() {
            return keywordScore;
        }

        public void setKeywordScore(Double keywordScore) {
            this.keywordScore = keywordScore;
        }

        public Double getSemanticScore() {
            return semanticScore;
        }

        public void setSemanticScore(Double semanticScore) {
            this.semanticScore = semanticScore;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public void setMatchedSkills(List<String> matchedSkills) {
            this.matchedSkills = matchedSkills;
        }

        public List<String> getDetectedSkills() {
            return detectedSkills;
        }

        public void setDetectedSkills(List<String> detectedSkills) {
            this.detectedSkills = detectedSkills;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getAnalysisSource() {
            return analysisSource;
        }

        public void setAnalysisSource(String analysisSource) {
            this.analysisSource = analysisSource;
        }
    }
}
