# Recruiter Analytics Dashboard - Implementation Notes

> **Date**: April 26, 2026  
> **Context**: Created shared analytics fragment for Admin and Recruiter dashboards

---

## Overview

Implemented a reusable analytics system that provides metrics and charts to both Admin and Recruiter dashboards. Recruiters see only their own data while Admins see all data.

---

## Architecture

```
fragments/analytics.html
├── metrics fragment    → Total Resumes, Total Jobs, Average Score
├── charts fragment     → Top Skills bar chart, Score Distribution doughnut
└── chartScripts       → Chart.js initialization code

        ↓ (used by)

├── admin.html         → Full dashboard (metrics + charts + table)
└── recruiter.html     → Metrics + charts only (no table)
```

---

## Files Created

### 1. fragments/analytics.html
Reusable Thymeleaf fragment with three parts:
- **`metrics`**: Three metric cards (Total Resumes, Total Jobs, Average Score)
- **`charts`**: Two chart containers (Top Skills, Score Distribution)
- **`chartScripts`**: Chart.js initialization for both charts

### 2. RecruiterDashboardService.java
Service that returns recruiter-scoped data:
```java
public record RecruiterDashboardData(
    long totalResumes,      // Resumes from recruiter's results
    long totalJobs,         // Jobs from recruiter's results
    double averageScore,    // Average score of recruiter's results
    List<SkillFrequency> topMatchedSkills,
    long maxSkillFrequency,
    ScoreDistribution scoreDistribution
)
```

Key method:
```java
public RecruiterDashboardData getDashboardData(String username)
```
- Filters results by `ownerUsername`
- Uses existing repository method: `findByOwnerUsernameOrderByCreatedAtDesc()`

### 3. RecruiterController.java
Simple controller:
```java
@GetMapping("/recruiter")
public String recruiterDashboard(Model model, Authentication authentication) {
    String username = authentication.getName();
    model.addAttribute("analytics", recruiterDashboardService.getDashboardData(username));
    return "recruiter";
}
```

### 4. recruiter.html
Recruiter dashboard page that:
- Uses `analytics` model attribute (not `dashboard` like admin)
- Includes metrics + charts + skill trends (no table)
- Imports the analytics fragment

---

## Files Modified

### SecurityConfig.java
Two changes:

1. **Route permission**:
   ```java
   .requestMatchers("/", "/result", "/analyze", "/recruiter").hasAnyRole("RECRUITER", "ADMIN")
   ```

2. **Login redirect**:
   ```java
   response.sendRedirect(isAdmin ? "/admin" : "/recruiter");
   ```
   (was `/` before)

---

## Data Model Differences

| Field | Admin | Recruiter |
|-------|-------|-----------|
| Model attribute | `dashboard` | `analytics` |
| Recent Rankings Table | ✅ Yes | ❌ No |
| Data Scope | All results | Own results only |
| Endpoint | `/admin` | `/recruiter` |

---

## Key Decisions

1. **New `/recruiter` page** (not added to home or history)
2. **User-scoped data** (only their own results)
3. **Metrics + charts only** (no table)

---

## To Verify

Run the application and:
1. Login as a recruiter user → should land on `/recruiter`
2. Verify data is filtered to own results only
3. Check charts render correctly
4. Login as admin → should land on `/admin` (unchanged)

---

## Future Enhancements

- Add recent activity table to recruiter dashboard (optional)
- Add "View Details" modal for recruiter results
- Consider unifying the model attribute name (`dashboard` vs `analytics`)