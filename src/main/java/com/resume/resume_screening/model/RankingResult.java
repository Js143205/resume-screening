package com.resume.resume_screening.model;

import com.resume.resume_screening.auth.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ranking_results")
public class RankingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id")
    private AppUser owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    private double score;
    private double skillScore;
    private double keywordScore;
    private double semanticScore;

    @Lob
    private String matchedSkills;

    @Lob
    private String explanation;

    private LocalDateTime createdAt;

    public RankingResult() {
    }

    public RankingResult(AppUser owner,
                         Resume resume,
                         JobDescription jobDescription,
                         double score,
                         double skillScore,
                         double keywordScore,
                         double semanticScore,
                         String matchedSkills,
                         String explanation,
                         LocalDateTime createdAt) {
        this.owner = owner;
        this.resume = resume;
        this.jobDescription = jobDescription;
        this.score = score;
        this.skillScore = skillScore;
        this.keywordScore = keywordScore;
        this.semanticScore = semanticScore;
        this.matchedSkills = matchedSkills;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Resume getResume() {
        return resume;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public double getScore() {
        return score;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public double getKeywordScore() {
        return keywordScore;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public String getExplanation() {
        return explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
