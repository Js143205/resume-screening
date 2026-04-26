package com.resume.resume_screening.repository;

import com.resume.resume_screening.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByOrderByUploadedAtDesc();
}
