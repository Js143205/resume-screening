package com.resume.resume_screening.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final SignupService signupService;
    private final PasswordResetService passwordResetService;

    public LoginController(SignupService signupService, PasswordResetService passwordResetService) {
        this.signupService = signupService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/signup")
    public String signupPost(@RequestParam String username, @RequestParam String password) {
        try {
            signupService.signupRecruiter(username, password);
            return "redirect:/login?signup";
        } catch (IllegalStateException e) {
            return "redirect:/signup?error=" + urlEncode(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordPost(
            @RequestParam("identifier") String identifier,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword
    ) {
        try {
            passwordResetService.resetPassword(identifier, newPassword, confirmPassword);
            return "redirect:/login?resetSuccess";
        } catch (IllegalStateException e) {
            return "redirect:/forgot-password?error=" + urlEncode(e.getMessage());
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "Signup%20failed";
        }
    }
}

