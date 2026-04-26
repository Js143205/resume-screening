package com.resume.resume_screening.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equals(auth.getName())) {
            log.warn("Rejected request because no valid authenticated principal was available. principal={}",
                    auth == null ? null : auth.getName());
            throw new IllegalStateException("No authenticated user found.");
        }

        String username = auth.getName();
        log.info("Resolving authenticated AppUser for principal={}", username);

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user record not found for username: " + username));

        validateUserId(user.getId());
        log.info("Authenticated AppUser resolved. username={}, userId={}", user.getUsername(), user.getId());
        return user;
    }

    public AppUser requireUserById(Long id) {
        validateUserId(id);
        log.info("Resolving AppUser by explicit id={}", id);
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unable to find AppUser with id " + id));
    }

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            log.warn("Invalid AppUser id received: {}", id);
            throw new IllegalArgumentException("Invalid user ID");
        }
    }
}
