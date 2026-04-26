package com.resume.resume_screening.service;

import com.resume.resume_screening.client.PythonNlpClient;
import com.resume.resume_screening.auth.AppUser;
import com.resume.resume_screening.auth.CurrentUserService;
import com.resume.resume_screening.model.JobDescription;
import com.resume.resume_screening.model.RankingResult;
import com.resume.resume_screening.model.Resume;
import com.resume.resume_screening.repository.JobDescriptionRepository;
import com.resume.resume_screening.repository.RankingResultRepository;
import com.resume.resume_screening.repository.ResumeRepository;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private static final int MAX_FILES = 10;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final double SKILL_WEIGHT = 0.45;
    private static final double KEYWORD_WEIGHT = 0.30;
    private static final double SEMANTIC_WEIGHT = 0.25;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");
    private static final Set<String> STOP_WORDS = Set.of(
            "for", "with", "and", "the", "a", "an", "looking", "to", "of", "in", "on",
            "at", "by", "from", "or", "as", "is", "are", "be", "will", "should", "can"
    );
    private static final String[] KNOWN_SKILLS = {
            "java", "spring", "spring boot", "mysql", "sql", "python", "django", "api",
            "rest", "hibernate", "jpa", "javascript", "html", "css", "react", "docker",
            "microservices", "kafka", "aws", "git", "bootstrap", "thymeleaf", "hibernate",
            "mongodb", "postgresql", "linux", "excel", "machine learning"
    };

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final RankingResultRepository rankingResultRepository;
    private final PythonNlpClient pythonNlpClient;
    private final CurrentUserService currentUserService;
    private final ThreadLocal<Tika> tika = ThreadLocal.withInitial(Tika::new);

    public ResumeAnalysisService(ResumeRepository resumeRepository,
                                 JobDescriptionRepository jobDescriptionRepository,
                                 RankingResultRepository rankingResultRepository,
                                 PythonNlpClient pythonNlpClient,
                                 CurrentUserService currentUserService) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.rankingResultRepository = rankingResultRepository;
        this.pythonNlpClient = pythonNlpClient;
        this.currentUserService = currentUserService;
    }

    public AnalysisResult analyze(String jobTitle, List<MultipartFile> files, String jobDesc) {
        try {
            AppUser owner = currentUserService.requireCurrentUser();
            List<MultipartFile> uploadedFiles = validateFiles(files);
            String resolvedJobTitle = resolveJobTitle(jobTitle);
            String normalizedJobDesc = validateJobDescription(jobDesc);

            JobDescription savedJobDescription = jobDescriptionRepository.save(
                    new JobDescription(resolvedJobTitle, normalizedJobDesc, LocalDateTime.now())
            );

            List<RankedResume> rankedResumes = new ArrayList<>();
            try {
                int poolSize = Math.max(1, Math.min(uploadedFiles.size(), Runtime.getRuntime().availableProcessors()));
                ExecutorService executor = Executors.newFixedThreadPool(poolSize);
                try {
                    List<CompletableFuture<RankedResume>> futures = new ArrayList<>();
                    int fileIndex = 1;
                    for (MultipartFile file : uploadedFiles) {
                        int indexForName = fileIndex;
                        MultipartFile fileForTask = file;
                        futures.add(CompletableFuture.supplyAsync(() -> processSingleFile(fileForTask, indexForName, normalizedJobDesc), executor));
                        fileIndex++;
                    }

                    rankedResumes = futures.stream()
                            .map(future -> {
                                try {
                                    return future.join();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.error("Async resume processing orchestration failed.", e);
                                    return null;
                                }
                            })
                            .filter(resume -> resume != null)
                            .collect(Collectors.toCollection(ArrayList::new));
                } finally {
                    executor.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("Async processing failed. Falling back to sequential processing.", e);
                rankedResumes = processSequentially(uploadedFiles, normalizedJobDesc);
            }

            log.info("Ranked resumes size: {}", rankedResumes.size());

            // Persist sequentially to avoid JPA concurrency issues.
            for (RankedResume rankedResume : rankedResumes) {
                ParsedResume parsedResume = rankedResume.parsedResume();
                ScoreDetails scoreDetails = rankedResume.scoreDetails();

                Resume savedResume = resumeRepository.save(
                        new Resume(parsedResume.fileName(), parsedResume.contentType(), parsedResume.extractedText(), LocalDateTime.now())
                );
                rankingResultRepository.save(buildRankingResult(owner, savedResume, savedJobDescription, scoreDetails));
            }

            rankedResumes.sort(Comparator
                    .comparingDouble((RankedResume rankedResume) -> rankedResume.scoreDetails().finalScore()).reversed()
                    .thenComparing(rankedResume -> rankedResume.parsedResume().fileName(), String.CASE_INSENSITIVE_ORDER));

            List<AnalysisResult.ResumeInsight> insights = new ArrayList<>();
            int rank = 1;
            for (RankedResume rankedResume : rankedResumes) {
                ScoreDetails details = rankedResume.scoreDetails();
                ParsedResume parsedResume = rankedResume.parsedResume();
                insights.add(new AnalysisResult.ResumeInsight(
                        rank,
                        parsedResume.fileName(),
                        parsedResume.contentType(),
                        Math.round(details.finalScore()),
                        new AnalysisResult.ScoreBreakdown(
                                Math.round(details.skillScore()),
                                Math.round(details.keywordScore()),
                                Math.round(details.semanticScore())
                        ),
                        details.matchedSkills(),
                        details.detectedSkills(),
                        parsedResume.extractedText(),
                        details.explanation(),
                        details.analysisSource()
                ));
                rank++;
            }

            String best = insights.isEmpty()
                    ? "No resumes were analyzed"
                    : insights.size() == 1
                    ? insights.get(0).getFileName() + " was analyzed successfully"
                    : insights.get(0).getFileName() + " is the best match";

            AnalysisResult analysisResult = new AnalysisResult(resolvedJobTitle, insights, best);
            log.info("Final AnalysisResult: {}", analysisResult);
            return analysisResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Error in analyze: " + e.getMessage(), e);
        }
    }

    private RankedResume processSingleFile(MultipartFile file, int fileIndex, String normalizedJobDesc) {
        try {
            ParsedResume parsedResume = parseResume(file, "Resume " + fileIndex);
            ScoreDetails scoreDetails = scoreResume(parsedResume.extractedText(), normalizedJobDesc);
            return new RankedResume(parsedResume, scoreDetails);
        } catch (Exception e) {
            e.printStackTrace();
            String fileName = file == null ? null : file.getOriginalFilename();
            log.error("Failed processing file: {}", fileName, e);
            return null;
        }
    }

    private List<RankedResume> processSequentially(List<MultipartFile> uploadedFiles, String normalizedJobDesc) {
        List<RankedResume> rankedResumes = new ArrayList<>();
        int fileIndex = 1;
        for (MultipartFile file : uploadedFiles) {
            RankedResume rankedResume = processSingleFile(file, fileIndex, normalizedJobDesc);
            if (rankedResume != null) {
                rankedResumes.add(rankedResume);
            }
            fileIndex++;
        }
        return rankedResumes;
    }

    public AnalysisResult analyze(String jobTitle, MultipartFile file1, MultipartFile file2, String jobDesc) {
        return analyze(jobTitle, Arrays.asList(file1, file2), jobDesc);
    }

    private ParsedResume parseResume(MultipartFile file, String fallbackFileName) {
        if (file == null || file.isEmpty()) {
            return new ParsedResume(fallbackFileName, "text/plain", "");
        }

        try {
            String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                    ? fallbackFileName
                    : file.getOriginalFilename();
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            String extractedText = tika.get().parseToString(file.getInputStream()).trim();
            return new ParsedResume(fileName, contentType, extractedText);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract text from uploaded file: " + file.getOriginalFilename(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Unsupported or unreadable file: " + file.getOriginalFilename(), e);
        }
    }



    /**
     * PRIMARY + FALLBACK scoring architecture.
     * Tries Python NLP first; if it fails (timeout, exception, null/incomplete response), falls back to Java-only scoring.
     * NEVER returns null - always returns a valid ScoreDetails.
     */
    private ScoreDetails scoreResume(String resumeText, String jobDesc) {
        // Step 1: Calculate Java scores as baseline
        ScoreDetails javaScore = calculateScore(resumeText, jobDesc);
        Set<String> jdSkills = safeSet(extractSkills(jobDesc));

        // Step 2: Try Python NLP FIRST (PRIMARY)
        try {
            log.info("Attempting Python NLP scoring for resume...");
            PythonNlpClient.PythonNlpResult pythonResult = pythonNlpClient.analyze(resumeText, jobDesc);

            // Step 3: Validate Python success
            if (isPythonResultValid(pythonResult)) {
                log.info("Python NLP succeeded. Building result from Python response.");
                return buildFromPython(pythonResult, javaScore, jdSkills);
            } else {
                String reason = getPythonFailureReason(pythonResult);
                log.warn("Python NLP returned invalid result: {}. Switching to Java fallback.", reason);
                return buildFromJavaFallback(javaScore, "Python failed: " + reason);
            }
        } catch (Exception e) {
            // Step 4: Python threw exception - FALLBACK to Java
            log.warn("Python NLP threw exception, switching to Java fallback.", e);
            return buildFromJavaFallback(javaScore, "Python exception: " + e.getMessage());
        }
    }

    /**
     * Strict validation of Python result.
     * Returns true only if Python response is COMPLETE and usable.
     */
    private boolean isPythonResultValid(PythonNlpClient.PythonNlpResult pythonResult) {
        if (pythonResult == null) {
            return false;
        }
        if (!pythonResult.hasResponse()) {
            return false;
        }
        PythonNlpClient.PythonNlpResponse response = pythonResult.response();
        if (response == null) {
            return false;
        }
        // Check for critical fields - all must be present and non-null
        List<String> missing = findMissingPythonFields(response);
        return missing.isEmpty(); // Strict: must have ALL fields
    }

    private String getPythonFailureReason(PythonNlpClient.PythonNlpResult pythonResult) {
        if (pythonResult == null) {
            return "Python result wrapper is null";
        }
        if (!pythonResult.hasResponse()) {
            return pythonResult.message();
        }
        if (pythonResult.response() == null) {
            return "Python response is null";
        }
        List<String> missing = findMissingPythonFields(pythonResult.response());
        if (!missing.isEmpty()) {
            return "Missing critical fields: " + missing;
        }
        return "Unknown failure";
    }

    /**
     * Build ScoreDetails from Python NLP response (PRIMARY path).
     * Uses complete Python data; falls back to Java for any missing values.
     */
    private ScoreDetails buildFromPython(PythonNlpClient.PythonNlpResult pythonResult,
                                          ScoreDetails javaScore,
                                          Set<String> jdSkills) {
        PythonNlpClient.PythonNlpResponse python = pythonResult.response();

        double skillScore = firstNonNull(python.getSkillScore(), javaScore.skillScore());
        double keywordScore = firstNonNull(python.getKeywordScore(), javaScore.keywordScore());
        double semanticScore = firstNonNull(python.getSemanticScore(), javaScore.semanticScore());
        double finalScore = python.getFinalScore() != null
                ? python.getFinalScore()
                : weightedScore(skillScore, keywordScore, semanticScore);

        List<String> matchedSkills = mergeSkills(python.getMatchedSkills(), javaScore.matchedSkills());
        List<String> detectedSkills = mergeSkills(python.getDetectedSkills(), javaScore.detectedSkills());

        String explanation = buildStructuredExplanation(
                skillScore, keywordScore, semanticScore,
                matchedSkills, jdSkills
        );
        if (python.getExplanation() != null && !python.getExplanation().isBlank()) {
            explanation = explanation + " (NLP Insight: " + python.getExplanation() + ")";
        }

        log.info("Successfully built result from Python NLP. Final score: {}", finalScore);
        return new ScoreDetails(
                finalScore,
                skillScore,
                keywordScore,
                semanticScore,
                matchedSkills,
                detectedSkills,
                explanation,
                "python_nlp"
        );
    }

    /**
     * Build ScoreDetails from Java-only scoring (FALLBACK path).
     * Always returns valid ScoreDetails - never null.
     */
    private ScoreDetails buildFromJavaFallback(ScoreDetails javaScore, String fallbackReason) {
        log.warn("Using Java fallback scoring. Reason: {}", fallbackReason);

        // Ensure no null values in fallback
        double finalScore = javaScore.finalScore();
        double skillScore = javaScore.skillScore();
        double keywordScore = javaScore.keywordScore();
        double semanticScore = javaScore.semanticScore();
        List<String> matchedSkills = safeList(javaScore.matchedSkills());
        List<String> detectedSkills = safeList(javaScore.detectedSkills());
        String explanation = javaScore.explanation() != null
                ? javaScore.explanation() + " [Java fallback]"
                : "Scored using Java fallback due to Python service unavailability.";

        return new ScoreDetails(
                finalScore,
                skillScore,
                keywordScore,
                semanticScore,
                matchedSkills,
                detectedSkills,
                explanation,
                "java_fallback"
        );
    }

    private ScoreDetails calculateScore(String resume, String jobDesc) {

        String resumeText = normalize(resume);
        String jdText = normalize(jobDesc);
        List<String> resumeTokens = safeList(tokenize(resumeText));
        List<String> jdTokens = safeList(tokenize(jdText));

        Set<String> jdSkills = safeSet(extractSkills(jdText));
        Set<String> resumeSkills = safeSet(extractSkills(resumeText));
        List<String> matchedSkills = new ArrayList<>();

        for (String skill : jdSkills) {
            if (resumeSkills.contains(skill)) {
                matchedSkills.add(skill);
            }
        }

        double skillScore = jdSkills.isEmpty() ? 0 : ((double) matchedSkills.size() / jdSkills.size()) * 100;
        double keywordScore = calculateKeywordScore(resumeTokens, jdTokens);
        double semanticScore = calculateSemanticScore(resumeTokens, jdTokens);
        double finalScore = weightedScore(skillScore, keywordScore, semanticScore);

        Set<String> missingSkills = new LinkedHashSet<>(jdSkills);
        missingSkills.removeAll(matchedSkills);

        String explanation = buildStructuredExplanation(
                skillScore,
                keywordScore,
                semanticScore,
                matchedSkills,
                missingSkills
        );
        return new ScoreDetails(
                finalScore,
                skillScore,
                keywordScore,
                semanticScore,
                safeList(matchedSkills),
                new ArrayList<>(resumeSkills),
                explanation,
                "java_fallback"
        );
    }

    private ScoreDetails appendFallbackReason(ScoreDetails javaScore, String reason) {
        return new ScoreDetails(
                javaScore.finalScore(),
                javaScore.skillScore(),
                javaScore.keywordScore(),
                javaScore.semanticScore(),
                javaScore.matchedSkills(),
                javaScore.detectedSkills(),
                javaScore.explanation() + " Java fallback reason: " + reason,
                "java_fallback"
        );
    }

    private List<String> findMissingPythonFields(PythonNlpClient.PythonNlpResponse pythonResponse) {
        List<String> missingFields = new ArrayList<>();
        if (pythonResponse == null) {
            missingFields.add("pythonResponse");
            return missingFields;
        }
        if (pythonResponse.getFinalScore() == null) {
            missingFields.add("finalScore");
        }
        if (pythonResponse.getSkillScore() == null) {
            missingFields.add("skillScore");
        }
        if (pythonResponse.getKeywordScore() == null) {
            missingFields.add("keywordScore");
        }
        if (pythonResponse.getSemanticScore() == null) {
            missingFields.add("semanticScore");
        }
        if (pythonResponse.getMatchedSkills() == null) {
            missingFields.add("matchedSkills");
        }
        if (pythonResponse.getDetectedSkills() == null) {
            missingFields.add("detectedSkills");
        }
        if (pythonResponse.getExplanation() == null || pythonResponse.getExplanation().isBlank()) {
            missingFields.add("explanation");
        }
        return missingFields;
    }

    private ScoreDetails mergeScores(ScoreDetails javaScore,
                                     PythonNlpClient.PythonNlpResponse pythonResponse,
                                     List<String> missingFields,
                                     Set<String> jdSkills) {
        if (pythonResponse == null) {
            return appendFallbackReason(javaScore, "Python NLP response was null.");
        }

        List<String> safeMissingFields = safeList(missingFields);
        Set<String> safeJdSkills = safeSet(jdSkills);
        List<String> safeJavaMatchedSkills = javaScore == null ? List.of() : safeList(javaScore.matchedSkills());
        List<String> safeJavaDetectedSkills = javaScore == null ? List.of() : safeList(javaScore.detectedSkills());

        double skillScore = firstNonNull(pythonResponse.getSkillScore(), javaScore.skillScore());
        double keywordScore = firstNonNull(pythonResponse.getKeywordScore(), javaScore.keywordScore());
        double semanticScore = firstNonNull(pythonResponse.getSemanticScore(), javaScore.semanticScore());
        double finalScore = pythonResponse.getFinalScore() != null
                ? pythonResponse.getFinalScore()
                : weightedScore(skillScore, keywordScore, semanticScore);

        List<String> matchedSkills = mergeSkills(pythonResponse.getMatchedSkills(), safeJavaMatchedSkills);
        List<String> detectedSkills = mergeSkills(pythonResponse.getDetectedSkills(), safeJavaDetectedSkills);

        String analysisSource = safeMissingFields.isEmpty()
                ? normalizeAnalysisSource(pythonResponse.getAnalysisSource(), "python_nlp")
                : "python_nlp+java_fallback";

        Set<String> missingSkills = new LinkedHashSet<>(safeJdSkills);
        missingSkills.removeAll(safeJavaMatchedSkills);

        String explanation = buildStructuredExplanation(
                javaScore.skillScore(),
                javaScore.keywordScore(),
                javaScore.semanticScore(),
                safeJavaMatchedSkills,
                missingSkills
        );

        String pythonExplanation = pythonResponse.getExplanation();
        if (pythonExplanation != null && !pythonExplanation.isBlank()) {
            explanation = explanation + " (NLP Insight: " + pythonExplanation + ")";
        }

        return new ScoreDetails(
                finalScore,
                skillScore,
                keywordScore,
                semanticScore,
                matchedSkills,
                detectedSkills,
                explanation,
                analysisSource
        );
    }

    private String normalizeAnalysisSource(String analysisSource, String defaultSource) {
        return analysisSource == null || analysisSource.isBlank() ? defaultSource : analysisSource;
    }

    private List<String> mergeSkills(List<String> pythonSkills, List<String> javaSkills) {
        LinkedHashSet<String> mergedSkills = new LinkedHashSet<>();
        if (pythonSkills != null) {
            mergedSkills.addAll(pythonSkills.stream().filter(skill -> skill != null && !skill.isBlank()).toList());
        }
        if (javaSkills != null) {
            mergedSkills.addAll(javaSkills.stream().filter(skill -> skill != null && !skill.isBlank()).toList());
        }
        return new ArrayList<>(mergedSkills);
    }

    private double firstNonNull(Double preferredValue, double fallbackValue) {
        return preferredValue != null ? preferredValue : fallbackValue;
    }

    private double weightedScore(double skillScore, double keywordScore, double semanticScore) {
        return (skillScore * SKILL_WEIGHT)
                + (keywordScore * KEYWORD_WEIGHT)
                + (semanticScore * SEMANTIC_WEIGHT);
    }

    private double calculateKeywordScore(List<String> resumeTokens, List<String> jdTokens) {
        List<String> safeResumeTokens = safeList(resumeTokens);
        List<String> safeJdTokens = safeList(jdTokens);

        Set<String> jdKeywords = new LinkedHashSet<>(safeJdTokens);
        if (jdKeywords.isEmpty()) {
            return 0;
        }

        Set<String> resumeKeywords = new HashSet<>(safeResumeTokens);
        long overlap = jdKeywords.stream().filter(resumeKeywords::contains).count();
        return ((double) overlap / jdKeywords.size()) * 100;
    }

    private double calculateSemanticScore(List<String> resumeTokens, List<String> jdTokens) {
        List<String> safeResumeTokens = safeList(resumeTokens);
        List<String> safeJdTokens = safeList(jdTokens);

        if (safeResumeTokens.isEmpty() || safeJdTokens.isEmpty()) {
            return 0;
        }

        Map<String, Integer> resumeFreq = buildFrequencyMap(safeResumeTokens);
        Map<String, Integer> jdFreq = buildFrequencyMap(safeJdTokens);

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(resumeFreq.keySet());
        vocabulary.addAll(jdFreq.keySet());

        double dotProduct = 0;
        double resumeMagnitude = 0;
        double jdMagnitude = 0;

        for (String token : vocabulary) {
            int resumeValue = resumeFreq.getOrDefault(token, 0);
            int jdValue = jdFreq.getOrDefault(token, 0);
            dotProduct += resumeValue * jdValue;
            resumeMagnitude += resumeValue * resumeValue;
            jdMagnitude += jdValue * jdValue;
        }

        if (resumeMagnitude == 0 || jdMagnitude == 0) {
            return 0;
        }

        return (dotProduct / (Math.sqrt(resumeMagnitude) * Math.sqrt(jdMagnitude))) * 100;
    }

    private Set<String> extractSkills(String text) {
        String normalizedText = normalize(text);
        List<String> tokens = safeList(tokenize(normalizedText));

        Set<String> skills = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            String canonicalSkill = canonicalizeSkillName(skill);
            List<String> skillTokens = Arrays.stream(canonicalSkill.split("\\s+"))
                    .filter(s -> !s.isBlank())
                    .toList();

            if (skillTokens.isEmpty()) {
                continue;
            }

            boolean matched;
            if (skillTokens.size() == 1) {
                matched = tokens.contains(skillTokens.get(0));
            } else {
                matched = containsConsecutiveTokens(tokens, skillTokens);
            }

            if (matched) {
                skills.add(canonicalSkill);
            }
        }

        return skills;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9+#. ]", " ");
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String raw : text.split("\\s+")) {
            String token = raw == null ? "" : raw.trim();
            if (token.isBlank()) continue;

            // Synonym normalization for skill matching + token similarity.
            if ("js".equals(token)) {
                token = "javascript";
            } else if ("reactjs".equals(token)) {
                token = "react";
            } else if ("spring".equals(token)) {
                addTokenIfValid(tokens, "spring");
                addTokenIfValid(tokens, "boot");
                continue;
            }

            addTokenIfValid(tokens, token);
        }
        return tokens;
    }

    private void addTokenIfValid(List<String> tokens, String token) {
        if (tokens == null || token == null) return;
        String trimmed = token.trim();
        if (trimmed.isBlank()) return;
        if (trimmed.length() <= 1) return;
        if (STOP_WORDS.contains(trimmed)) return;
        tokens.add(trimmed);
    }

    private boolean containsConsecutiveTokens(List<String> tokens, List<String> required) {
        if (tokens == null || required == null) {
            return false;
        }
        if (required.isEmpty() || tokens.isEmpty() || required.size() > tokens.size()) {
            return false;
        }

        for (int i = 0; i <= tokens.size() - required.size(); i++) {
            boolean allMatch = true;
            for (int j = 0; j < required.size(); j++) {
                if (!tokens.get(i + j).equals(required.get(j))) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) return true;
        }
        return false;
    }

    private String canonicalizeSkillName(String skill) {
        if (skill == null) return "";
        String trimmedLower = skill.trim().toLowerCase();
        // Synonym normalization requested in Phase 3.
        if ("spring".equals(trimmedLower)) {
            return "spring boot";
        }
        return trimmedLower;
    }

    private Map<String, Integer> buildFrequencyMap(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : safeList(tokens)) {
            frequencies.merge(token, 1, Integer::sum);
        }
        return frequencies;
    }

    private RankingResult buildRankingResult(AppUser owner, Resume resume, JobDescription jobDescription, ScoreDetails scoreDetails) {
        return new RankingResult(
                owner,
                resume,
                jobDescription,
                scoreDetails.finalScore(),
                scoreDetails.skillScore(),
                scoreDetails.keywordScore(),
                scoreDetails.semanticScore(),
                String.join(", ", safeList(scoreDetails.matchedSkills())),
                scoreDetails.explanation(),
                LocalDateTime.now()
        );
    }

    private String resolveJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) {
            return "Untitled Job Description";
        }
        return jobTitle.trim();
    }

    private String validateJobDescription(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalStateException("Job description is required.");
        }
        return jobDescription.trim();
    }

    private List<MultipartFile> validateFiles(List<MultipartFile> files) {
        List<MultipartFile> nonEmptyFiles = files == null
                ? List.of()
                : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (nonEmptyFiles.isEmpty()) {
            throw new IllegalStateException("Please upload at least one resume file.");
        }

        if (nonEmptyFiles.size() > MAX_FILES) {
            throw new IllegalStateException("You can upload up to " + MAX_FILES + " resumes at a time.");
        }

        for (MultipartFile file : nonEmptyFiles) {
            validateSingleFile(file);
        }

        return nonEmptyFiles;
    }

    private void validateSingleFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalStateException("Unsupported file type for " + safeFileName(originalFilename)
                    + ". Allowed types: PDF, DOC, DOCX, TXT.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalStateException("File " + safeFileName(originalFilename)
                    + " exceeds the 5 MB upload limit.");
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "uploaded file" : fileName;
    }

    private String buildStructuredExplanation(double skillScore,
                                              double keywordScore,
                                              double semanticScore,
                                              List<String> matchedSkills,
                                              Set<String> missingSkills) {
        List<String> topMatched = safeList(matchedSkills).stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .distinct()
                .limit(5)
                .toList();

        String strengthsSkillsText = formatSkills(topMatched);
        String strengthsSentence = skillScore >= 70
                ? "Strong in " + strengthsSkillsText
                : skillScore >= 40
                ? "Some strengths in " + strengthsSkillsText
                : "Limited strengths in " + strengthsSkillsText;

        String weaknessesSentence;
        if (missingSkills == null || missingSkills.isEmpty()) {
            weaknessesSentence = "No obvious skill gaps.";
        } else {
            List<String> topMissing = safeSet(missingSkills).stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .limit(5)
                    .toList();
            weaknessesSentence = "Missing " + formatSkills(topMissing) + ".";
        }

        String keywordSentence = keywordAlignmentLabel(keywordScore) + " keyword alignment.";
        String semanticSentence = semanticSimilarityLabel(semanticScore) + " contextual similarity.";

        return strengthsSentence + ". " + weaknessesSentence + " " + keywordSentence + " " + semanticSentence;
    }

    private String keywordAlignmentLabel(double keywordScore) {
        if (keywordScore >= 80) return "Excellent";
        if (keywordScore >= 60) return "Strong";
        if (keywordScore >= 40) return "Good";
        if (keywordScore >= 20) return "Low";
        return "Poor";
    }

    private String semanticSimilarityLabel(double semanticScore) {
        if (semanticScore >= 80) return "Very high";
        if (semanticScore >= 60) return "High";
        if (semanticScore >= 40) return "Moderate";
        if (semanticScore >= 20) return "Low";
        return "Minimal";
    }

    private String formatSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "none";
        }

        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(ResumeAnalysisService::displaySkill)
                .collect(Collectors.joining(", "));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private Set<String> safeSet(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private static String displaySkill(String skill) {
        if (skill == null) {
            return "";
        }

        String trimmed = skill.trim().toLowerCase();
        return switch (trimmed) {
            case "sql" -> "SQL";
            case "api" -> "API";
            case "jpa" -> "JPA";
            case "aws" -> "AWS";
            case "html" -> "HTML";
            case "css" -> "CSS";
            default -> Arrays.stream(trimmed.split("\\s+"))
                    .filter(part -> !part.isBlank())
                    .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                    .collect(Collectors.joining(" "));
        };
    }

    private record ScoreDetails(double finalScore,
                                double skillScore,
                                double keywordScore,
                                double semanticScore,
                                List<String> matchedSkills,
                                List<String> detectedSkills,
                                String explanation,
                                String analysisSource) {
    }

    private record ParsedResume(String fileName, String contentType, String extractedText) {
    }

    private record RankedResume(ParsedResume parsedResume, ScoreDetails scoreDetails) {
    }
}

