package com.resume.resume_screening;

import jakarta.persistence.*;

@Entity
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double score;

    public Candidate() {}

    public Candidate(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public String getName() { return name; }
    public double getScore() { return score; }

    public void setName(String name) { this.name = name; }
    public void setScore(double score) { this.score = score; }
}

