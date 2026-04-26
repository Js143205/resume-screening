package com.resume.resume_screening.service;

import com.resume.resume_screening.model.JobDescription;
import com.resume.resume_screening.model.RankingResult;
import com.resume.resume_screening.model.Resume;
import com.resume.resume_screening.repository.JobDescriptionRepository;
import com.resume.resume_screening.repository.RankingResultRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PersistenceQueryService {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeRepository resumeRepository;
    private final RankingResultRepository rankingResultRepository;

    public PersistenceQueryService(JobDescriptionRepository jobDescriptionRepository,
                                   ResumeRepository resumeRepository,
                                   RankingResultRepository rankingResultRepository) {
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.resumeRepository = resumeRepository;
        this.rankingResultRepository = rankingResultRepository;
    }

    public List<JobSummary> getJobs() {
        return jobDescriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(job -> new JobSummary(job.getId(), job.getTitle(), job.getDescription(), job.getCreatedAt()))
                .toList();
    }

    public List<ResumeSummary> getResumes() {
        return resumeRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(resume -> new ResumeSummary(
                        resume.getId(),
                        resume.getFileName(),
                        resume.getContentType(),
                        resume.getUploadedAt(),
                        resume.getExtractedText() == null ? 0 : resume.getExtractedText().length()
                ))
                .toList();
    }

    public List<RankingResultSummary> getRankingHistory() {
        return rankingResultRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapRankingResult)
                .toList();
    }

    public List<RankingResultSummary> getRankingHistoryForUser(String username) {
        return rankingResultRepository.findByOwner_UsernameOrderByCreatedAtDesc(username).stream()
                .map(this::mapRankingResult)
                .toList();
    }

    public List<RankingResultSummary> getRankingHistoryForJob(Long jobDescriptionId) {
        return rankingResultRepository.findByJobDescriptionIdOrderByScoreDesc(jobDescriptionId).stream()
                .map(this::mapRankingResult)
                .toList();
    }

    public List<RankingResultSummary> getRankingHistoryForUserAndJob(String username, Long jobDescriptionId) {
        return rankingResultRepository.findByOwner_UsernameAndJobDescriptionIdOrderByScoreDesc(username, jobDescriptionId).stream()
                .map(this::mapRankingResult)
                .toList();
    }

    public List<AnalysisHistoryRow> getHistoryRows(boolean isAdmin, String username) {
        List<RankingResult> source = isAdmin
                ? rankingResultRepository.findAllByOrderByCreatedAtDesc()
                : rankingResultRepository.findByOwner_UsernameOrderByCreatedAtDesc(username);

        Map<Long, List<RankingResult>> groupedByJob = source.stream()
                .filter(r -> r.getJobDescription() != null && r.getJobDescription().getId() != null)
                .collect(Collectors.groupingBy(r -> r.getJobDescription().getId()));

        return groupedByJob.values().stream()
                .map(this::toHistoryRow)
                .sorted(Comparator.comparing(AnalysisHistoryRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Optional<AnalysisResult> buildAnalysisResultForJobId(Long jobDescriptionId, boolean isAdmin, String username) {
        List<RankingResult> rankingResults = isAdmin
                ? rankingResultRepository.findByJobDescriptionIdOrderByScoreDesc(jobDescriptionId)
                : rankingResultRepository.findByOwner_UsernameAndJobDescriptionIdOrderByScoreDesc(username, jobDescriptionId);

        if (rankingResults == null || rankingResults.isEmpty()) {
            return Optional.empty();
        }

        String jobTitle = Optional.ofNullable(rankingResults.get(0).getJobDescription())
                .map(JobDescription::getTitle)
                .orElse("Untitled Job Description");

        List<AnalysisResult.ResumeInsight> insights = new ArrayList<>();
        int rank = 1;
        for (RankingResult rankingResult : rankingResults) {
            Resume resume = rankingResult.getResume();
            String resumeFileName = resume == null ? "Resume" : resume.getFileName();
            String contentType = resume == null ? "application/octet-stream" : resume.getContentType();
            String resumeText = resume == null ? "" : resume.getExtractedText();
            List<String> matchedSkills = splitSkills(rankingResult.getMatchedSkills());

            insights.add(new AnalysisResult.ResumeInsight(
                    rank,
                    resumeFileName,
                    contentType,
                    Math.round(rankingResult.getScore()),
                    new AnalysisResult.ScoreBreakdown(
                            Math.round(rankingResult.getSkillScore()),
                            Math.round(rankingResult.getKeywordScore()),
                            Math.round(rankingResult.getSemanticScore())
                    ),
                    matchedSkills,
                    List.of(),
                    resumeText,
                    rankingResult.getExplanation(),
                    "persisted_result"
            ));
            rank++;
        }

        String best = insights.isEmpty()
                ? "No resumes were analyzed"
                : insights.get(0).getFileName() + " is the best match";

        return Optional.of(new AnalysisResult(jobTitle, insights, best));
    }

    private AnalysisHistoryRow toHistoryRow(List<RankingResult> rankingsForJob) {
        long resumeCount = rankingsForJob.size();
        double topScore = rankingsForJob.stream()
                .mapToDouble(RankingResult::getScore)
                .max()
                .orElse(0);
        LocalDateTime createdAt = rankingsForJob.stream()
                .map(RankingResult::getCreatedAt)
                .max(Comparator.nullsLast(Comparator.naturalOrder()))
                .orElse(null);

        RankingResult reference = rankingsForJob.get(0);
        Long jobId = reference.getJobDescription().getId();
        String jobTitle = Optional.ofNullable(reference.getJobDescription().getTitle()).orElse("Untitled Job Description");

        return new AnalysisHistoryRow(
                jobId,
                createdAt,
                jobTitle,
                resumeCount,
                Math.round(topScore)
        );
    }

    private RankingResultSummary mapRankingResult(RankingResult rankingResult) {
        Resume resume = rankingResult.getResume();
        JobDescription jobDescription = rankingResult.getJobDescription();

        return new RankingResultSummary(
                rankingResult.getId(),
                resume.getId(),
                resume.getFileName(),
                jobDescription.getId(),
                jobDescription.getTitle(),
                rankingResult.getScore(),
                rankingResult.getSkillScore(),
                rankingResult.getKeywordScore(),
                rankingResult.getSemanticScore(),
                splitSkills(rankingResult.getMatchedSkills()),
                splitSkills(rankingResult.getMatchedSkills()).size(),
                rankingResult.getExplanation(),
                rankingResult.getCreatedAt()
        );
    }

    private List<String> splitSkills(String matchedSkills) {
        if (matchedSkills == null || matchedSkills.isBlank()) {
            return List.of();
        }

        return List.of(matchedSkills.split(",")).stream()
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .toList();
    }

    public record JobSummary(Long id, String title, String description, java.time.LocalDateTime createdAt) {
    }

    public record ResumeSummary(Long id,
                                String fileName,
                                String contentType,
                                java.time.LocalDateTime uploadedAt,
                                int extractedTextLength) {
    }

    public record RankingResultSummary(Long id,
                                       Long resumeId,
                                       String resumeFileName,
                                       Long jobDescriptionId,
                                       String jobTitle,
                                       double score,
                                       double skillScore,
                                       double keywordScore,
                                       double semanticScore,
                                       List<String> matchedSkills,
                                       int matchedSkillCount,
                                       String explanation,
                                       java.time.LocalDateTime createdAt) {
    }

    public record AnalysisHistoryRow(Long id,
                                     java.time.LocalDateTime createdAt,
                                     String jobTitle,
                                     long resumeCount,
                                     long topScore) {
    }
}
