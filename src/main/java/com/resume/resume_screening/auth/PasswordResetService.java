package com.resume.resume_screening.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetPassword(String usernameOrEmail, String newPassword, String confirmPassword) {
        String normalizedIdentifier = usernameOrEmail == null ? "" : usernameOrEmail.trim();
        if (normalizedIdentifier.isBlank()) {
            throw new IllegalStateException("Username or email is required.");
        }

        String password = newPassword == null ? "" : newPassword;
        if (password.isBlank()) {
            throw new IllegalStateException("New password is required.");
        }
        if (password.length() < 6 || password.length() > 72) {
            throw new IllegalStateException("Password must be 6 to 72 characters.");
        }
        if (!password.equals(confirmPassword == null ? "" : confirmPassword)) {
            throw new IllegalStateException("Passwords do not match.");
        }

        AppUser user = appUserRepository.findByUsernameIgnoreCase(normalizedIdentifier)
                .orElseThrow(() -> new IllegalStateException("No account found for that username/email."));

        user.setPasswordHash(passwordEncoder.encode(password));
        appUserRepository.save(user);
    }
}
