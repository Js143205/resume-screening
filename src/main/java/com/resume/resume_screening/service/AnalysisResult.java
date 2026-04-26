package com.resume.resume_screening.service;

import java.util.List;
import java.util.Objects;

public class AnalysisResult {

    private final String jobTitle;
    private final int totalResumes;
    private final String best;
    private final List<ResumeInsight> rankedResumes;

    public AnalysisResult(String jobTitle, List<ResumeInsight> rankedResumes, String best) {
        this.jobTitle = jobTitle == null || jobTitle.isBlank() ? "Untitled Job Description" : jobTitle;
        this.rankedResumes = rankedResumes == null ? List.of() : List.copyOf(rankedResumes);
        this.totalResumes = this.rankedResumes.size();
        this.best = best == null || best.isBlank() ? "No resumes were analyzed" : best;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public int getTotalResumes() {
        return totalResumes;
    }

    public String getBest() {
        return best;
    }

    public List<ResumeInsight> getRankedResumes() {
        return rankedResumes;
    }

    public ResumeInsight getResume1() {
        return rankedResumes.isEmpty() ? null : rankedResumes.get(0);
    }

    public ResumeInsight getResume2() {
        return rankedResumes.size() < 2 ? null : rankedResumes.get(1);
    }

    public static class ResumeInsight {
        private final int rank;
        private final String fileName;
        private final String contentType;
        private final long score;
        private final ScoreBreakdown breakdown;
        private final List<String> matchedSkills;
        private final List<String> detectedSkills;
        private final String resumeText;
        private final String explanation;
        private final String analysisSource;

        public ResumeInsight(int rank,
                             String fileName,
                             String contentType,
                             long score,
                             ScoreBreakdown breakdown,
                             List<String> matchedSkills,
                             List<String> detectedSkills,
                             String resumeText,
                             String explanation,
                             String analysisSource) {
            this.rank = rank;
            this.fileName = fileName == null || fileName.isBlank() ? "Resume" : fileName;
            this.contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
            this.score = score;
            this.breakdown = breakdown == null ? new ScoreBreakdown(0, 0, 0) : breakdown;
            this.matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
            this.detectedSkills = detectedSkills == null ? List.of() : List.copyOf(detectedSkills);
            this.resumeText = resumeText == null ? "" : resumeText;
            this.explanation = explanation == null || explanation.isBlank() ? "No explanation available." : explanation;
            this.analysisSource = analysisSource == null || analysisSource.isBlank() ? "java_fallback" : analysisSource;
        }

        public int getRank() {
            return rank;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public long getScore() {
            return score;
        }

        public ScoreBreakdown getBreakdown() {
            return breakdown;
        }

        public long getSkillScore() {
            return breakdown.skillScore();
        }

        public long getKeywordScore() {
            return breakdown.keywordScore();
        }

        public long getSemanticScore() {
            return breakdown.semanticScore();
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public List<String> getDetectedSkills() {
            return detectedSkills;
        }

        public String getResumeText() {
            return resumeText;
        }

        public String getExplanation() {
            return explanation;
        }

        public String getAnalysisSource() {
            return analysisSource;
        }

        @Override
        public String toString() {
            return "ResumeInsight{" +
                    "rank=" + rank +
                    ", fileName='" + fileName + '\'' +
                    ", score=" + score +
                    ", matchedSkills=" + matchedSkills +
                    ", analysisSource='" + analysisSource + '\'' +
                    '}';
        }
    }

    public record ScoreBreakdown(long skillScore, long keywordScore, long semanticScore) {
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "jobTitle='" + jobTitle + '\'' +
                ", totalResumes=" + totalResumes +
                ", best='" + best + '\'' +
                ", rankedResumes=" + Objects.toString(rankedResumes) +
                '}';
    }
}
