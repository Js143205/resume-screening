package com.resume.resume_screening.controller;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.resume.resume_screening.model.RankingResult;
import com.resume.resume_screening.repository.RankingResultRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ReportApiController {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final RankingResultRepository rankingResultRepository;

    public ReportApiController(RankingResultRepository rankingResultRepository) {
        this.rankingResultRepository = rankingResultRepository;
    }

    @GetMapping(value = "/report/{rankingId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long rankingId, Authentication authentication) {
        RankingResult rankingResult = rankingResultRepository.findById(rankingId)
                .orElseThrow(() -> new IllegalStateException("Ranking result not found."));

        if (!canDownload(authentication, rankingResult)) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdfBytes = generatePdf(rankingResult);

        String fileName = "resume-screening-report-" + rankingId + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private boolean canDownload(Authentication authentication, RankingResult rankingResult) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }
        if (rankingResult.getOwner() == null || rankingResult.getOwner().getUsername() == null) {
            return false;
        }
        return rankingResult.getOwner().getUsername().equals(authentication.getName());
    }

    private byte[] generatePdf(RankingResult rankingResult) {
        String jobTitle = rankingResult.getJobDescription() != null
                ? Objects.toString(rankingResult.getJobDescription().getTitle(), "")
                : "";
        String resumeFileName = rankingResult.getResume() != null
                ? Objects.toString(rankingResult.getResume().getFileName(), "")
                : "";
        String explanation = rankingResult.getExplanation() == null || rankingResult.getExplanation().isBlank()
                ? "No explanation available."
                : rankingResult.getExplanation();

        List<String> matchedSkills = splitSkills(rankingResult.getMatchedSkills());
        LocalDateTime createdAt = rankingResult.getCreatedAt();
        String timestamp = createdAt == null ? "-" : createdAt.format(TS_FORMATTER);

        double score = rankingResult.getScore();
        double skillScore = rankingResult.getSkillScore();
        double keywordScore = rankingResult.getKeywordScore();
        double semanticScore = rankingResult.getSemanticScore();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Resume Screening Report", titleFont));
            document.add(Paragraph.getInstance(" "));

            document.add(new Paragraph("Job Title: " + (jobTitle.isBlank() ? "-" : jobTitle)));
            document.add(new Paragraph("Resume File: " + (resumeFileName.isBlank() ? "-" : resumeFileName)));
            document.add(Paragraph.getInstance(" "));

            document.add(new Paragraph("Final Score: " + Math.round(score) + "%", sectionFont));
            document.add(new Paragraph("Score Breakdown:", sectionFont));
            document.add(new Paragraph("Skill Score: " + Math.round(skillScore) + "%"));
            document.add(new Paragraph("Keyword Score: " + Math.round(keywordScore) + "%"));
            document.add(new Paragraph("Semantic Score: " + Math.round(semanticScore) + "%"));

            document.add(Paragraph.getInstance(" "));
            document.add(new Paragraph("Matched Skills:", sectionFont));
            document.add(new Paragraph(matchedSkills.isEmpty() ? "-" : String.join(", ", matchedSkills)));

            document.add(Paragraph.getInstance(" "));
            document.add(new Paragraph("Explanation:", sectionFont));
            document.add(new Paragraph(explanation));

            document.add(Paragraph.getInstance(" "));
            document.add(new Paragraph("Generated At: " + timestamp));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF report.", e);
        }
    }

    private List<String> splitSkills(String matchedSkills) {
        if (matchedSkills == null || matchedSkills.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(matchedSkills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .toList();
    }
}

