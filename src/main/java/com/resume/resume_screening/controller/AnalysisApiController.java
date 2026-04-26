package com.resume.resume_screening.controller;

import jakarta.servlet.http.HttpSession;
import com.resume.resume_screening.service.AnalysisResult;
import com.resume.resume_screening.service.ResumeAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalysisApiController {

    private final ResumeAnalysisService resumeAnalysisService;

    public AnalysisApiController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResult analyze(@RequestParam(required = false) MultipartFile[] files,
                                  @RequestParam(required = false) MultipartFile file1,
                                  @RequestParam(required = false) MultipartFile file2,
                                  @RequestParam(required = false) String jobTitle,
                                  @RequestParam String jobDesc,
                                  HttpSession session) {
        AnalysisResult result = resumeAnalysisService.analyze(jobTitle, mergeFiles(files, file1, file2), jobDesc);
        session.setAttribute("latestAnalysisResult", result);
        session.setAttribute("latestJobTitle", HomeController.resolveJobTitle(jobTitle));
        return result;
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
}
