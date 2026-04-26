package com.resume.resume_screening.service;

import com.resume.resume_screening.model.RankingResult;
import com.resume.resume_screening.repository.JobDescriptionRepository;
import com.resume.resume_screening.repository.RankingResultRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final RankingResultRepository rankingResultRepository;

    public AdminDashboardService(ResumeRepository resumeRepository,
                                 JobDescriptionRepository jobDescriptionRepository,
                                 RankingResultRepository rankingResultRepository) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.rankingResultRepository = rankingResultRepository;
    }

    public AdminDashboardData getDashboardData() {
        long totalResumes = safeCount(resumeRepository.count());
        long totalJobs = safeCount(jobDescriptionRepository.count());
        List<RankingResult> rankingResults = Optional.ofNullable(rankingResultRepository.findAllByOrderByCreatedAtDesc())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (rankingResults.isEmpty()) {
            return AdminDashboardData.empty(totalResumes, totalJobs);
        }

        OptionalDouble averageScoreOptional = rankingResults.stream()
                .mapToDouble(RankingResult::getScore)
                .average();
        double averageScore = averageScoreOptional.orElse(0.0);

        List<RecentRankingRow> recentRankings = rankingResults.stream()
                .limit(10)
                .map(result -> new RecentRankingRow(
                        result.getId(),
                        resolveJobTitle(result),
                        resolveResumeFileName(result),
                        Math.round(result.getScore()),
                        resolveCreatedAt(result)
                ))
                .toList();

        List<SkillFrequency> topMatchedSkills = rankingResults.stream()
                .map(RankingResult::getMatchedSkills)
                .filter(skills -> skills != null && !skills.isBlank())
                .flatMap(skills -> List.of(skills.split(",")).stream())
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new SkillFrequency(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingLong(SkillFrequency::count).reversed()
                        .thenComparing(SkillFrequency::skill))
                .limit(12)
                .toList();

        long maxSkillFrequency = topMatchedSkills.stream()
                .mapToLong(SkillFrequency::count)
                .max()
                .orElse(0L);

        ScoreDistribution scoreDistribution = new ScoreDistribution(
                countScoresInRange(rankingResults, 0, 40),
                countScoresInRange(rankingResults, 40, 60),
                countScoresInRange(rankingResults, 60, 80),
                countScoresInRange(rankingResults, 80, 101)
        );

        return new AdminDashboardData(
                totalResumes,
                totalJobs,
                averageScore,
                recentRankings,
                topMatchedSkills,
                maxSkillFrequency,
                scoreDistribution
        );
    }

    public RankingResultDetails getRankingResultDetails(Long rankingResultId) {
        RankingResult rankingResult = rankingResultRepository.findById(rankingResultId)
                .orElseThrow(() -> new IllegalStateException("Ranking result not found."));

        return new RankingResultDetails(
                rankingResult.getId(),
                resolveJobTitle(rankingResult),
                resolveResumeFileName(rankingResult),
                Math.round(rankingResult.getScore()),
                Math.round(rankingResult.getSkillScore()),
                Math.round(rankingResult.getKeywordScore()),
                Math.round(rankingResult.getSemanticScore()),
                splitSkills(rankingResult.getMatchedSkills()),
                Optional.ofNullable(rankingResult.getExplanation()).filter(text -> !text.isBlank()).orElse("No explanation available."),
                resolveCreatedAt(rankingResult)
        );
    }

    public DeleteResultResponse deleteRankingResult(Long rankingResultId) {
        if (!rankingResultRepository.existsById(rankingResultId)) {
            throw new IllegalStateException("Ranking result not found.");
        }

        rankingResultRepository.deleteById(rankingResultId);
        return new DeleteResultResponse(rankingResultId, "Ranking result deleted successfully.");
    }

    private long countScoresInRange(List<RankingResult> rankingResults, int lowerInclusive, int upperExclusive) {
        return rankingResults.stream()
                .mapToDouble(RankingResult::getScore)
                .filter(score -> score >= lowerInclusive && score < upperExclusive)
                .count();
    }

    private long safeCount(long value) {
        return Math.max(value, 0L);
    }

    private String resolveJobTitle(RankingResult rankingResult) {
        return Optional.ofNullable(rankingResult.getJobDescription())
                .map(jobDescription -> jobDescription.getTitle() == null || jobDescription.getTitle().isBlank()
                        ? "Untitled Job"
                        : jobDescription.getTitle())
                .orElse("Untitled Job");
    }

    private String resolveResumeFileName(RankingResult rankingResult) {
        return Optional.ofNullable(rankingResult.getResume())
                .map(resume -> resume.getFileName() == null || resume.getFileName().isBlank()
                        ? "Unnamed Resume"
                        : resume.getFileName())
                .orElse("Unnamed Resume");
    }

    private LocalDateTime resolveCreatedAt(RankingResult rankingResult) {
        return Optional.ofNullable(rankingResult.getCreatedAt()).orElse(null);
    }

    private List<String> splitSkills(String matchedSkills) {
        if (matchedSkills == null || matchedSkills.isBlank()) {
            return Collections.emptyList();
        }

        return List.of(matchedSkills.split(",")).stream()
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .toList();
    }

    public record AdminDashboardData(long totalResumes,
                                     long totalJobs,
                                     double averageScore,
                                     List<RecentRankingRow> recentRankings,
                                     List<SkillFrequency> topMatchedSkills,
                                     long maxSkillFrequency,
                                     ScoreDistribution scoreDistribution) {
        public AdminDashboardData {
            recentRankings = recentRankings == null ? Collections.emptyList() : List.copyOf(recentRankings);
            topMatchedSkills = topMatchedSkills == null ? Collections.emptyList() : List.copyOf(topMatchedSkills);
            maxSkillFrequency = Math.max(maxSkillFrequency, 0L);
            scoreDistribution = scoreDistribution == null ? new ScoreDistribution(0, 0, 0, 0) : scoreDistribution;
        }

        public static AdminDashboardData empty(long totalResumes, long totalJobs) {
            return new AdminDashboardData(
                    totalResumes,
                    totalJobs,
                    0.0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0L,
                    new ScoreDistribution(0, 0, 0, 0)
            );
        }
    }

    public record RecentRankingRow(Long id,
                                   String jobTitle,
                                   String resumeFileName,
                                   long score,
                                   java.time.LocalDateTime createdAt) {
    }

    public record SkillFrequency(String skill, long count) {
    }

    public record ScoreDistribution(long range0To40,
                                    long range40To60,
                                    long range60To80,
                                    long range80To100) {
    }

    public record RankingResultDetails(Long id,
                                       String jobTitle,
                                       String resumeFileName,
                                       long score,
                                       long skillScore,
                                       long keywordScore,
                                       long semanticScore,
                                       List<String> matchedSkills,
                                       String explanation,
                                       LocalDateTime createdAt) {
    }

    public record DeleteResultResponse(Long id, String message) {
    }
}
