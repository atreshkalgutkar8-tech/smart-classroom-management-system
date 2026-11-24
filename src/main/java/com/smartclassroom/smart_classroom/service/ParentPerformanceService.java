package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.entity.GameType;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.GameAssignmentRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ParentPerformanceService {

    private static final List<String> SUBJECTS =
            List.of("Mathematics", "Kannada", "English", "EVS");

    private final UserRepository userRepository;
    private final GameAssignmentRepository assignmentRepository;
    private final GameAttemptRepository attemptRepository;

    public ParentPerformanceService(
            UserRepository userRepository,
            GameAssignmentRepository assignmentRepository,
            GameAttemptRepository attemptRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public ParentDashboard getDashboard(String parentEmail, Long selectedChildId) {
        User parent = getParent(parentEmail);
        List<ChildOption> children = parent.getChildren().stream()
                .filter(child -> child.getRole() == Role.STUDENT)
                .sorted(Comparator.comparing(User::getFullName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(ChildOption::from)
                .toList();

        if (selectedChildId == null) {
            return new ParentDashboard(children, null);
        }
        User selected = parent.getChildren().stream()
                .filter(child -> child.getId().equals(selectedChildId)
                        && child.getRole() == Role.STUDENT)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "That student is not linked to your parent account."));
        return new ParentDashboard(children, buildPerformance(selected));
    }

    @Transactional(readOnly = true)
    public ChildPerformance getChildPerformance(String parentEmail, Long childId) {
        User parent = getParent(parentEmail);
        User child = parent.getChildren().stream()
                .filter(candidate -> candidate.getId().equals(childId)
                        && candidate.getRole() == Role.STUDENT)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "That student is not linked to your parent account."));
        return buildPerformance(child);
    }

    private User getParent(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> user.getRole() == Role.PARENT)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parent account not found."));
    }

    private ChildPerformance buildPerformance(User child) {
        List<GameAssignment> assignments = child.getGrade() == null
                || child.getSection() == null
                ? List.of()
                : assignmentRepository.findByGradeAndSectionIgnoreCaseAndActiveTrue(
                        child.getGrade(), child.getSection().trim());
        List<Long> assignmentIds = assignments.stream()
                .map(GameAssignment::getId).toList();
        List<GameAttempt> attempts = assignmentIds.isEmpty()
                ? List.of()
                : attemptRepository.findByAssignment_IdInAndStudent_Id(
                        assignmentIds, child.getId());

        Map<Long, List<GameAttempt>> attemptsByAssignment = new LinkedHashMap<>();
        attempts.forEach(attempt -> attemptsByAssignment
                .computeIfAbsent(attempt.getAssignment().getId(), ignored -> new ArrayList<>())
                .add(attempt));

        Set<Long> completedAssignmentIds = attempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCompleted()))
                .map(attempt -> attempt.getAssignment().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<GameHistory> history = assignments.stream()
                .map(assignment -> history(assignment,
                        attemptsByAssignment.getOrDefault(assignment.getId(), List.of())))
                .sorted((left, right) -> {
                    LocalDateTime leftDate = left.latestCompletion();
                    LocalDateTime rightDate = right.latestCompletion();
                    if (leftDate == null && rightDate != null) return 1;
                    if (leftDate != null && rightDate == null) return -1;
                    if (leftDate != null) {
                        int byDate = rightDate.compareTo(leftDate);
                        if (byDate != 0) return byDate;
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            left.title(), right.title());
                })
                .toList();

        List<GameAttempt> completedAttempts = attempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCompleted()))
                .filter(attempt -> attempt.getTotalQuestions() != null
                        && attempt.getTotalQuestions() > 0)
                .toList();
        List<Double> percentages = completedAttempts.stream()
                .map(ParentPerformanceService::percentage)
                .toList();

        List<SubjectPerformance> subjectPerformance = SUBJECTS.stream()
                .map(subject -> subjectPerformance(subject, assignments,
                        attempts, completedAssignmentIds))
                .toList();

        return new ChildPerformance(
                ChildOption.from(child), assignments.size(),
                completedAssignmentIds.size(),
                assignments.size() - completedAssignmentIds.size(),
                attempts.size(),
                maximum(percentages),
                percentages.isEmpty() ? null
                        : round(percentages.stream().mapToDouble(Double::doubleValue)
                                .average().orElse(0)),
                assignments.isEmpty() ? null
                        : round(100.0 * completedAssignmentIds.size() / assignments.size()),
                subjectPerformance, history);
    }

    private SubjectPerformance subjectPerformance(
            String subject,
            List<GameAssignment> assignments,
            List<GameAttempt> attempts,
            Set<Long> completedAssignmentIds) {
        List<GameAssignment> subjectAssignments = assignments.stream()
                .filter(assignment -> subject.equalsIgnoreCase(
                        assignment.getGame().getSubject()))
                .toList();
        Set<Long> subjectAssignmentIds = subjectAssignments.stream()
                .map(GameAssignment::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<Double> completedPercentages = attempts.stream()
                .filter(attempt -> subjectAssignmentIds.contains(
                        attempt.getAssignment().getId()))
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCompleted()))
                .filter(attempt -> attempt.getTotalQuestions() != null
                        && attempt.getTotalQuestions() > 0)
                .map(ParentPerformanceService::percentage)
                .toList();
        long completedGames = subjectAssignmentIds.stream()
                .filter(completedAssignmentIds::contains).count();
        return new SubjectPerformance(subject, subjectAssignments.size(),
                (int) completedGames,
                maximum(completedPercentages),
                completedPercentages.isEmpty() ? null
                        : round(completedPercentages.stream()
                                .mapToDouble(Double::doubleValue).average().orElse(0)));
    }

    private GameHistory history(
            GameAssignment assignment, List<GameAttempt> attempts) {
        List<GameAttempt> completed = attempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCompleted()))
                .filter(attempt -> attempt.getTotalQuestions() != null
                        && attempt.getTotalQuestions() > 0)
                .toList();
        Double best = completed.stream()
                .map(ParentPerformanceService::percentage)
                .max(Double::compareTo).orElse(null);
        LocalDateTime latestCompletion = completed.stream()
                .map(GameAttempt::getCompletedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null);
        return new GameHistory(assignment.getGame().getTitle(),
                assignment.getGame().getGameType(),
                assignment.getGame().getSubject(), assignment.getGrade(),
                best, attempts.size(), !completed.isEmpty(), latestCompletion);
    }

    private static double percentage(GameAttempt attempt) {
        return round(100.0 * attempt.getScore() / attempt.getTotalQuestions());
    }

    private static Double maximum(List<Double> values) {
        return values.stream().max(Double::compareTo).orElse(null);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record ParentDashboard(
            List<ChildOption> children, ChildPerformance selectedChild) {
    }

    public record ChildOption(
            Long id, String fullName, Integer grade, String section) {
        static ChildOption from(User user) {
            return new ChildOption(user.getId(), user.getFullName(),
                    user.getGrade(), user.getSection());
        }
    }

    public record ChildPerformance(
            ChildOption child,
            int totalAssignedGames,
            int completedGames,
            int pendingGames,
            int totalAttempts,
            Double bestScorePercentage,
            Double averageScorePercentage,
            Double completionPercentage,
            List<SubjectPerformance> subjects,
            List<GameHistory> history) {
    }

    public record SubjectPerformance(
            String subject,
            int assignedGames,
            int completedGames,
            Double bestScorePercentage,
            Double averageScorePercentage) {
    }

    public record GameHistory(
            String title,
            GameType gameType,
            String subject,
            Integer grade,
            Double bestScorePercentage,
            int attemptCount,
            boolean completed,
            LocalDateTime latestCompletion) {
    }
}
