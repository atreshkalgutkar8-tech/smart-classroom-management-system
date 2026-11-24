package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.ClassroomSession;
import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.entity.GameType;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.SessionStatus;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.ClassroomSessionRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Teacher-facing, assignment-scoped student and game performance data. */
@Service
public class TeacherStudentService {

    private static final List<String> SUBJECTS =
            List.of("Mathematics", "English", "Kannada", "EVS");

    private final UserRepository userRepository;
    private final GameAssignmentRepository assignmentRepository;
    private final GameAttemptRepository attemptRepository;
    private final ClassroomSessionRepository sessionRepository;

    public TeacherStudentService(UserRepository userRepository,
            GameAssignmentRepository assignmentRepository,
            GameAttemptRepository attemptRepository,
            ClassroomSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public StudentList getStudents(String teacherEmail, Integer gradeFilter,
            String sectionFilter, String nameFilter) {
        User teacher = requireTeacher(teacherEmail);
        List<GameAssignment> assignments = authorizedAssignments(teacher);
        Set<ClassKey> classes = assignments.stream()
                .map(a -> new ClassKey(a.getGrade(), normalize(a.getSection())))
                .collect(Collectors.toSet());
        List<Integer> grades = classes.stream().map(ClassKey::grade).distinct().toList();
        List<User> students = grades.isEmpty() ? List.of()
                : userRepository.findByRoleAndGradeIn(Role.STUDENT, grades).stream()
                    .filter(s -> classes.contains(new ClassKey(s.getGrade(), normalize(s.getSection()))))
                    .filter(s -> gradeFilter == null || s.getGrade().equals(gradeFilter))
                    .filter(s -> sectionFilter == null || sectionFilter.isBlank()
                            || normalize(s.getSection()).equals(normalize(sectionFilter)))
                    .filter(s -> nameFilter == null || nameFilter.isBlank()
                            || s.getFullName().toLowerCase(Locale.ROOT)
                                .contains(nameFilter.trim().toLowerCase(Locale.ROOT)))
                    .sorted(Comparator.comparing(User::getFullName,
                            String.CASE_INSENSITIVE_ORDER)).toList();

        List<Long> assignmentIds = assignments.stream().map(GameAssignment::getId).toList();
        List<GameAttempt> attempts = assignmentIds.isEmpty() ? List.of()
                : attemptRepository.findByAssignment_IdIn(assignmentIds).stream()
                    .filter(a -> students.stream().anyMatch(s -> s.getId().equals(a.getStudent().getId())))
                    .toList();
        Map<Long, List<GameAttempt>> byStudent = attempts.stream().collect(
                Collectors.groupingBy(a -> a.getStudent().getId()));
        List<StudentSummary> summaries = students.stream()
                .map(s -> summary(s, assignments.stream()
                                .filter(a -> a.getGrade().equals(s.getGrade())
                                        && normalize(a.getSection()).equals(normalize(s.getSection())))
                                .toList(), byStudent.getOrDefault(s.getId(), List.of())))
                .toList();
        return new StudentList(summaries, classes.stream().map(ClassKey::grade).distinct().sorted().toList(),
                classes.stream().map(ClassKey::section).distinct().sorted().toList());
    }

    @Transactional(readOnly = true)
    public StudentDetail getStudent(String teacherEmail, Long studentId) {
        User teacher = requireTeacher(teacherEmail);
        List<GameAssignment> assignments = authorizedAssignments(teacher);
        Set<ClassKey> classes = assignments.stream()
                .map(a -> new ClassKey(a.getGrade(), normalize(a.getSection())))
                .collect(Collectors.toSet());
        User student = userRepository.findByIdAndRole(studentId, Role.STUDENT)
                .filter(s -> classes.contains(new ClassKey(s.getGrade(), normalize(s.getSection()))))
                .orElseThrow(() -> new IllegalArgumentException("Student not found in your assigned classes."));

        List<GameAssignment> studentAssignments = assignments.stream()
                .filter(a -> a.getGrade().equals(student.getGrade())
                        && normalize(a.getSection()).equals(normalize(student.getSection())))
                .toList();
        List<Long> ids = studentAssignments.stream().map(GameAssignment::getId).toList();
        List<GameAttempt> attempts = ids.isEmpty() ? List.of()
                : attemptRepository.findByAssignment_IdInAndStudent_Id(ids, studentId);
        StudentSummary overview = summary(student, studentAssignments, attempts);

        List<GamePerformance> games = studentAssignments.stream()
                .map(a -> gamePerformance(a, attempts.stream()
                        .filter(attempt -> attempt.getAssignment().getId().equals(a.getId())).toList()))
                .sorted(Comparator.comparing(GamePerformance::assignedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<SubjectPerformance> subjects = SUBJECTS.stream()
                .map(subject -> subjectPerformance(subject, studentAssignments, attempts)).toList();
        List<PerformanceEvent> history = new ArrayList<>();
        for (GameAttempt attempt : attempts) {
            history.add(new PerformanceEvent(
                    attempt.getCompletedAt() != null ? attempt.getCompletedAt() : attempt.getStartedAt(),
                    attempt.getAssignment().getGame().getTitle(),
                    attempt.getAssignment().getGame().getSubject(),
                    attempt.getAssignment().getGame().getGameType().name(),
                    Boolean.TRUE.equals(attempt.getCompleted()) ? "Completed attempt" : "In progress",
                    Boolean.TRUE.equals(attempt.getCompleted()) && attempt.getTotalQuestions() != null
                            && attempt.getTotalQuestions() > 0
                            ? percentage(attempt.getScore(), attempt.getTotalQuestions()) : null,
                    attempt.getTimeTakenSeconds()));
        }
        List<SessionSummary> sessions = sessionRepository.findByTeacherEmail(teacher.getEmail()).stream()
                .filter(session -> session.getGrade().equals(student.getGrade()))
                .sorted(Comparator.comparing(ClassroomSession::getCreatedAt).reversed())
                .map(SessionSummary::from).toList();
        history.sort(Comparator.comparing(PerformanceEvent::date,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return new StudentDetail(overview, subjects, games, history, sessions);
    }

    private StudentSummary summary(User student, List<GameAssignment> assignments,
            List<GameAttempt> attempts) {
        long completed = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCompleted()))
                .map(a -> a.getAssignment().getId()).distinct().count();
        Double best = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCompleted()))
                .filter(a -> a.getTotalQuestions() != null && a.getTotalQuestions() > 0)
                .map(a -> percentage(a.getScore(), a.getTotalQuestions()))
                .max(Double::compareTo).orElse(null);
        List<ParentContact> parents = student.getParents().stream()
                .filter(parent -> parent.getRole() == Role.PARENT)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(parent -> new ParentContact(parent.getId(), parent.getFullName(), parent.getEmail())).toList();
        return new StudentSummary(student.getId(), student.getFullName(), student.getEmail(),
                student.getGrade(), student.getSection(), parents, assignments.size(),
                (int) completed, Math.max(0, assignments.size() - (int) completed),
                attempts.size(), best);
    }

    private GamePerformance gamePerformance(GameAssignment assignment, List<GameAttempt> attempts) {
        List<GameAttempt> completed = attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getCompleted()))
                .filter(a -> a.getTotalQuestions() != null && a.getTotalQuestions() > 0).toList();
        GameAttempt best = completed.stream().max(Comparator.comparingDouble(
                a -> (double) a.getScore() / a.getTotalQuestions())).orElse(null);
        GameAttempt latest = attempts.stream().max(Comparator.comparing(GameAttempt::getStartedAt,
                Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        return new GamePerformance(assignment.getId(), assignment.getGame().getTitle(),
                assignment.getGame().getGameType(), assignment.getGame().getSubject(),
                assignment.getGrade(), attempts.size(), !completed.isEmpty(),
                best == null ? null : percentage(best.getScore(), best.getTotalQuestions()),
                best == null ? null : best.getTimeTakenSeconds(),
                latest == null ? null : latest.getStartedAt(), assignment.getAssignedAt());
    }

    private SubjectPerformance subjectPerformance(String subject,
            List<GameAssignment> assignments, List<GameAttempt> attempts) {
        Set<Long> ids = assignments.stream().filter(a -> subject.equalsIgnoreCase(a.getGame().getSubject()))
                .map(GameAssignment::getId).collect(Collectors.toSet());
        List<GameAttempt> completed = attempts.stream().filter(a -> ids.contains(a.getAssignment().getId()))
                .filter(a -> Boolean.TRUE.equals(a.getCompleted()))
                .filter(a -> a.getTotalQuestions() != null && a.getTotalQuestions() > 0).toList();
        long completeGames = completed.stream().map(a -> a.getAssignment().getId()).distinct().count();
        List<Double> values = completed.stream().map(a -> percentage(a.getScore(), a.getTotalQuestions())).toList();
        return new SubjectPerformance(subject, ids.size(), (int) completeGames,
                values.stream().max(Double::compareTo).orElse(null),
                values.isEmpty() ? null : round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
    }

    private List<GameAssignment> authorizedAssignments(User teacher) {
        return assignmentRepository.findByTeacher_EmailAndActiveTrueOrderByAssignedAtDesc(teacher.getEmail());
    }

    private User requireTeacher(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Teacher account not found."));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static double percentage(int score, int total) {
        return round(100.0 * score / total);
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }

    private record ClassKey(Integer grade, String section) { }
    public record ParentContact(Long id, String fullName, String email) { }
    public record StudentSummary(Long id, String fullName, String email, Integer grade, String section,
            List<ParentContact> parents, int totalAssignedGames, int completedGames, int pendingGames,
            int totalAttempts, Double bestScorePercentage) { }
    public record StudentList(List<StudentSummary> students, List<Integer> grades, List<String> sections) { }
    public record SubjectPerformance(String subject, int assignedGames, int completedGames,
            Double bestScorePercentage, Double averageScorePercentage) { }
    public record GamePerformance(Long assignmentId, String title, GameType gameType, String subject,
            Integer grade, int attempts, boolean completed, Double bestScorePercentage,
            Integer bestTimeSeconds, LocalDateTime latestAttempt, LocalDateTime assignedAt) { }
    public record PerformanceEvent(LocalDateTime date, String title, String subject, String activityType,
            String status, Double scorePercentage, Integer timeTakenSeconds) { }
    public record SessionSummary(Long id, String title, String subject, String topic, SessionStatus status,
            LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime endedAt) {
        static SessionSummary from(ClassroomSession session) {
            return new SessionSummary(session.getId(), session.getTitle(), session.getSubject(),
                    session.getTopic(), session.getStatus(), session.getCreatedAt(),
                    session.getStartedAt(), session.getEndedAt());
        }
    }
    public record StudentDetail(StudentSummary student, List<SubjectPerformance> subjects,
            List<GamePerformance> games, List<PerformanceEvent> history,
            List<SessionSummary> gradeSessions) { }
}
