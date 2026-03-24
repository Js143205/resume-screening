package com.resume.resume_screening;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import com.resume.resume_screening.CandidateRepository;


@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam MultipartFile file1,
                          @RequestParam MultipartFile file2,
                          @RequestParam String jobDesc,
                          Model model) {

        // Fake parsing
        String resume1 = "Java Spring Boot MySQL";
        String resume2 = "Python Django API";

        double score1 = calculateScore(resume1, jobDesc);
        double score2 = calculateScore(resume2, jobDesc);

        repository.save(new Candidate("Resume 1", score1));
        repository.save(new Candidate("Resume 2", score2));

        model.addAttribute("score1", Math.round(score1));
        model.addAttribute("score2", Math.round(score2));

        model.addAttribute("best", score1 > score2 ? "Resume 1 is better" : "Resume 2 is better");

        return "result";
    }

    private double calculateScore(String resume, String jobDesc) {

        String[] stopWords = {"for", "with", "and", "the", "a", "an", "looking"};

        String resumeText = resume.toLowerCase();
        String[] jobWords = jobDesc.toLowerCase().split("\\s+");

        int matchCount = 0;
        int validWords = 0;

        for (String word : jobWords) {

            boolean isStopWord = false;

            for (String stop : stopWords) {
                if (word.equals(stop)) {
                    isStopWord = true;
                    break;
                }
            }

            if (!isStopWord) {
                validWords++;

                if (resumeText.contains(word)) {
                    matchCount++;
                }
            }
        }

        return ((double) matchCount / validWords) * 100;
    }

    private final CandidateRepository repository;

    public HomeController(CandidateRepository repository) {
        this.repository = repository;
    }
}

