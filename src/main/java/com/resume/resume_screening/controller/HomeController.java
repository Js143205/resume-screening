package com.resume.resume_screening.controller;

import jakarta.servlet.http.HttpSession;
import com.resume.resume_screening.service.AnalysisResult;
import com.resume.resume_screening.service.PersistenceQueryService;
import com.resume.resume_screening.service.ResumeAnalysisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private static final String LATEST_ANALYSIS_RESULT = "latestAnalysisResult";
    private static final String LATEST_JOB_TITLE = "latestJobTitle";

    private final ResumeAnalysisService resumeAnalysisService;
    private final PersistenceQueryService persistenceQueryService;

    public HomeController(ResumeAnalysisService resumeAnalysisService,
                          PersistenceQueryService persistenceQueryService) {
        this.resumeAnalysisService = resumeAnalysisService;
        this.persistenceQueryService = persistenceQueryService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }


    @GetMapping("/result")
    public String showResult(@RequestParam(required = false) Long id,
                             Model model,
                             HttpSession session,
                             Authentication authentication) {
        if (id != null) {
            boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
            String username = authentication != null ? authentication.getName() : null;
            return persistenceQueryService.buildAnalysisResultForJobId(id, isAdmin, username)
                    .map(result -> {
                        populateResultModel(model, result, result.getJobTitle());
                        return "result";
                    })
                    .orElseGet(() -> {
                        session.removeAttribute(LATEST_ANALYSIS_RESULT);
                        session.removeAttribute(LATEST_JOB_TITLE);
                        return "redirect:/history?notFound=1";
                    });
        }

        AnalysisResult result = (AnalysisResult) session.getAttribute(LATEST_ANALYSIS_RESULT);
        String jobTitle = (String) session.getAttribute(LATEST_JOB_TITLE);
        log.info("Result object: {}", result);

        if (result == null) {
            log.warn("No analysis result in session. Redirecting to home.");
            return "redirect:/";
        }

        log.info("Result ranked resumes size before model population: {}", result.getRankedResumes().size());
        populateResultModel(model, result, jobTitle);
        log.info("Result page model populated. jobTitle={}, totalResumes={}, best={}",
                model.getAttribute("jobTitle"),
                model.getAttribute("totalResumes"),
                model.getAttribute("best"));
        return "result";
    }

    @GetMapping("/history")
    public String history(Model model, Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        String username = authentication != null ? authentication.getName() : null;
        model.addAttribute("analyses", persistenceQueryService.getHistoryRows(isAdmin, username));
        return "history";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(required = false) MultipartFile[] files,
                          @RequestParam(required = false) MultipartFile file1,
                          @RequestParam(required = false) MultipartFile file2,
                          @RequestParam(required = false) String jobTitle,
                          @RequestParam String jobDesc,
                          Model model,
                          HttpSession session) {
        log.info("/analyze endpoint hit. jobTitle={} | filesCount={} | legacyFilesPresent={}",
                jobTitle,
                files != null ? files.length : 0,
                file1 != null || file2 != null);

        AnalysisResult result = resumeAnalysisService.analyze(jobTitle, mergeFiles(files, file1, file2), jobDesc);
        log.info("Final AnalysisResult from controller: {}", result);

        String resolvedJobTitle = resolveJobTitle(jobTitle);
        session.setAttribute(LATEST_ANALYSIS_RESULT, result);
        session.setAttribute(LATEST_JOB_TITLE, resolvedJobTitle);
        populateResultModel(model, result, resolvedJobTitle);
        return "result";
    }

    static void populateResultModel(Model model, AnalysisResult result, String jobTitle) {
        log.info("Populating result model. result={}, jobTitle={}", result, jobTitle);
        model.addAttribute("jobTitle", resolveJobTitle(jobTitle));
        model.addAttribute("totalResumes", result.getTotalResumes());
        model.addAttribute("rankedResumes", result.getRankedResumes());
        model.addAttribute("resume1", result.getResume1());
        model.addAttribute("resume2", result.getResume2());
        model.addAttribute("best", result.getBest());
    }

    static String resolveJobTitle(String jobTitle) {
        return jobTitle == null || jobTitle.isBlank() ? "Untitled Job Description" : jobTitle;
    }

    private List<MultipartFile> mergeFiles(MultipartFile[] files, MultipartFile file1, MultipartFile file2) {
        List<MultipartFile> mergedFiles = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null) {
                    mergedFiles.add(file);
                }
            }
        }
        if (file1 != null) {
            mergedFiles.add(file1);
        }
        if (file2 != null) {
            mergedFiles.add(file2);
        }
        return mergedFiles;
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
