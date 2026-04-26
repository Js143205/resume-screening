package com.resume.resume_screening.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void resetPasswordUpdatesHashForMatchingUser() {
        AppUser user = new AppUser("recruiter", "old-hash", Role.RECRUITER);
        given(appUserRepository.findByUsernameIgnoreCase("recruiter")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newSecret1")).willReturn("new-hash");

        passwordResetService.resetPassword("recruiter", "newSecret1", "newSecret1");

        assertEquals("new-hash", user.getPasswordHash());
        verify(appUserRepository).save(user);
    }

    @Test
    void resetPasswordRejectsWhenAccountIsUnknown() {
        given(appUserRepository.findByUsernameIgnoreCase(any())).willReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> passwordResetService.resetPassword("missing-user", "newSecret1", "newSecret1"));

        assertEquals("No account found for that username/email.", ex.getMessage());
    }
}
