package com.resume.resume_screening.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signupRecruiter(String username, String rawPassword) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isBlank()) {
            throw new IllegalStateException("Username is required.");
        }
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 60) {
            throw new IllegalStateException("Username must be 3 to 60 characters.");
        }
        if (!normalizedUsername.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalStateException("Username can contain letters, numbers, dot, underscore, and hyphen only.");
        }

        String password = rawPassword == null ? "" : rawPassword;
        if (password.isBlank()) {
            throw new IllegalStateException("Password is required.");
        }
        if (password.length() < 6 || password.length() > 72) {
            throw new IllegalStateException("Password must be 6 to 72 characters.");
        }

        if (appUserRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("Username is already taken.");
        }

        appUserRepository.save(new AppUser(
                normalizedUsername,
                passwordEncoder.encode(password),
                Role.RECRUITER
        ));
    }
}

