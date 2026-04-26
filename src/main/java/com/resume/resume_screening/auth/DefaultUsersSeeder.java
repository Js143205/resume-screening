package com.resume.resume_screening.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUsersSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUsersSeeder.class);
    private static final String DEFAULT_RECRUITER_USERNAME = "recruiter";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DefaultUsersSeeder(AppUserRepository appUserRepository,
                              PasswordEncoder passwordEncoder,
                              JdbcTemplate jdbcTemplate) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", Role.ADMIN);
        createIfMissing(DEFAULT_RECRUITER_USERNAME, "recruiter123", Role.RECRUITER);
        repairLegacyRankingOwners();
    }

    private void createIfMissing(String username, String rawPassword, Role role) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }
        appUserRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
    }

    private void repairLegacyRankingOwners() {
        try {
            AppUser defaultRecruiter = appUserRepository.findByUsername(DEFAULT_RECRUITER_USERNAME)
                    .orElseThrow(() -> new IllegalStateException("Default recruiter user is missing."));

            if (defaultRecruiter.getId() == null || defaultRecruiter.getId() <= 0) {
                log.warn("Skipping ranking owner repair because default recruiter has invalid id={}", defaultRecruiter.getId());
                return;
            }

            int updatedRows = jdbcTemplate.update(
                    "UPDATE ranking_results SET owner_user_id = ? WHERE owner_user_id IS NULL OR owner_user_id = 0",
                    defaultRecruiter.getId()
            );

            if (updatedRows > 0) {
                log.info("Repaired {} legacy ranking result owner reference(s) to recruiter user id={}.",
                        updatedRows, defaultRecruiter.getId());
            }
        } catch (Exception exception) {
            log.warn("Legacy ranking owner repair skipped: {}", exception.getMessage());
        }
    }
}
