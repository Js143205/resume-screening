package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findAllByOrderByCreatedAtDesc();
}
