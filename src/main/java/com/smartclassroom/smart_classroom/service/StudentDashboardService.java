package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.entity.GameAttempt;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentDashboardService {

    private final StudentGameService studentGameService;
    private final GameAttemptService gameAttemptService;
    private final DIYActivityService activityService;

    public StudentDashboardService(StudentGameService studentGameService,
            GameAttemptService gameAttemptService, DIYActivityService activityService) {
        this.studentGameService = studentGameService;
        this.gameAttemptService = gameAttemptService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public Dashboard getDashboard(String studentEmail) {
        List<GameAssignment> assignments = List.of();
        String classMessage = null;
        try {
            assignments = studentGameService.getAssignedGames(studentEmail);
        } catch (IllegalArgumentException exception) {
            classMessage = exception.getMessage();
        }

        List<GameAttempt> attempts = gameAttemptService.getCompletedAttemptsForStudent(studentEmail);
        Set<Long> assignmentIds = assignments.stream().map(GameAssignment::getId).collect(Collectors.toSet());
        List<GameAttempt> currentCompletedAttempts = attempts.stream()
                .filter(attempt -> assignmentIds.contains(attempt.getAssignment().getId())).toList();
        long completedGames = currentCompletedAttempts.stream()
                .map(attempt -> attempt.getAssignment().getId()).distinct().count();

        List<DIYActivityService.StudentActivity> activities = activityService.studentCatalogue(studentEmail);
        long completedActivities = activities.stream().filter(DIYActivityService.StudentActivity::completed).count();
        List<RecentActivity> recent = new ArrayList<>();
        currentCompletedAttempts.forEach(attempt -> recent.add(new RecentActivity(
                attempt.getAssignment().getGame().getTitle(), attempt.getAssignment().getGame().getSubject(),
                "Game completed", attempt.getCompletedAt(), attempt.getTotalQuestions() == null
                        || attempt.getTotalQuestions() == 0 ? null
                        : Math.round(1000.0 * attempt.getScore() / attempt.getTotalQuestions()) / 10.0)));
        activityService.studentCompletionHistory(studentEmail).forEach(activity -> recent.add(
                new RecentActivity(activity.title(), activity.subject(), "DIY activity completed",
                        activity.completedAt(), null)));
        recent.sort(Comparator.comparing(RecentActivity::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        Double progress = assignments.isEmpty() ? null
                : Math.round(1000.0 * completedGames / assignments.size()) / 10.0;
        return new Dashboard(assignments.size(), (int) completedGames, (int) completedActivities,
                progress, recent.stream().limit(5).toList(), classMessage);
    }

    public record RecentActivity(String title, String subject, String activityType,
            LocalDateTime occurredAt, Double scorePercentage) { }
    public record Dashboard(int assignedGames, int completedGames, int completedActivities,
            Double gameCompletionPercentage, List<RecentActivity> recentActivity, String classMessage) { }
}
