package com.resume.resume_screening.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            response.sendRedirect(isAdmin ? "/admin" : "/");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler roleBasedSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // Admin-only
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Recruiter + Admin (main flow + history APIs)
                        .requestMatchers("/", "/result", "/analyze").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("RECRUITER", "ADMIN")

                        .anyRequest().authenticated()
                )
                // The UI uses fetch() calls for /api/* and a DELETE under /admin/result/*.
                // Keeping CSRF enabled for form login while ignoring it for these JSON endpoints keeps changes minimal.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        .ignoringRequestMatchers("/admin/result/**")
                        .ignoringRequestMatchers(request -> HttpMethod.DELETE.matches(request.getMethod()) && request.getRequestURI().startsWith("/admin/result/"))
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleBasedSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

