package com.resume.resume_screening.service;

import com.resume.resume_screening.model.RankingResult;
import com.resume.resume_screening.repository.JobDescriptionRepository;
import com.resume.resume_screening.repository.RankingResultRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecruiterDashboardService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final RankingResultRepository rankingResultRepository;

    public RecruiterDashboardService(ResumeRepository resumeRepository,
                                     JobDescriptionRepository jobDescriptionRepository,
                                     RankingResultRepository rankingResultRepository) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.rankingResultRepository = rankingResultRepository;
    }

    public RecruiterDashboardData getDashboardData(String username) {
        if (username == null || username.isBlank()) {
            return RecruiterDashboardData.empty(0L, 0L);
        }

        List<RankingResult> rankingResults = Optional.ofNullable(
                        rankingResultRepository.findByOwner_UsernameOrderByCreatedAtDesc(username))
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .toList();

        long totalResumes = rankingResults.stream()
                .map(RankingResult::getResume)
                .filter(Objects::nonNull)
                .map(r -> r.getId())
                .distinct()
                .count();

        long totalJobs = rankingResults.stream()
                .map(RankingResult::getJobDescription)
                .filter(Objects::nonNull)
                .map(j -> j.getId())
                .distinct()
                .count();

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

        if (rankingResults.isEmpty()) {
            return RecruiterDashboardData.empty(totalResumes, totalJobs);
        }

        double averageScore = rankingResults.stream()
                .mapToDouble(RankingResult::getScore)
                .average()
                .orElse(0.0);

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

        return new RecruiterDashboardData(
                totalResumes,
                totalJobs,
                averageScore,
                recentRankings,
                topMatchedSkills,
                maxSkillFrequency,
                scoreDistribution
        );
    }

    public RankingResultDetails getRankingResultDetails(String username, Long rankingResultId) {
        RankingResult rankingResult = findOwnedRankingResult(username, rankingResultId);
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

    public DeleteResultResponse deleteRankingResult(String username, Long rankingResultId) {
        RankingResult rankingResult = findOwnedRankingResult(username, rankingResultId);
        rankingResultRepository.delete(rankingResult);
        return new DeleteResultResponse(rankingResultId, "Ranking result deleted successfully.");
    }

    private RankingResult findOwnedRankingResult(String username, Long rankingResultId) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Recruiter identity is missing.");
        }

        return rankingResultRepository.findByIdAndOwner_Username(rankingResultId, username)
                .orElseThrow(() -> new IllegalStateException("Ranking result not found."));
    }

    private long countScoresInRange(List<RankingResult> rankingResults, int lowerInclusive, int upperExclusive) {
        return rankingResults.stream()
                .mapToDouble(RankingResult::getScore)
                .filter(score -> score >= lowerInclusive && score < upperExclusive)
                .count();
    }

    private String resolveJobTitle(RankingResult rankingResult) {
        return rankingResult.getJobDescription() != null && rankingResult.getJobDescription().getTitle() != null && !rankingResult.getJobDescription().getTitle().isBlank()
                ? rankingResult.getJobDescription().getTitle()
                : "Untitled Job";
    }

    private String resolveResumeFileName(RankingResult rankingResult) {
        return rankingResult.getResume() != null && rankingResult.getResume().getFileName() != null && !rankingResult.getResume().getFileName().isBlank()
                ? rankingResult.getResume().getFileName()
                : "Unnamed Resume";
    }

    private LocalDateTime resolveCreatedAt(RankingResult rankingResult) {
        return rankingResult.getCreatedAt();
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

    public record RecruiterDashboardData(long totalResumes,
                                         long totalJobs,
                                         double averageScore,
                                         List<RecentRankingRow> recentRankings,
                                         List<SkillFrequency> topMatchedSkills,
                                         long maxSkillFrequency,
                                         ScoreDistribution scoreDistribution) {
        public RecruiterDashboardData {
            recentRankings = recentRankings == null ? Collections.emptyList() : List.copyOf(recentRankings);
            topMatchedSkills = topMatchedSkills == null ? Collections.emptyList() : List.copyOf(topMatchedSkills);
            maxSkillFrequency = Math.max(maxSkillFrequency, 0L);
            scoreDistribution = scoreDistribution == null ? new ScoreDistribution(0, 0, 0, 0) : scoreDistribution;
        }

        public static RecruiterDashboardData empty(long totalResumes, long totalJobs) {
            return new RecruiterDashboardData(
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
                                   LocalDateTime createdAt) {
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
