
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.GameAssignmentRepository;
import com.smartclassroom.smart_classroom.repository.GameRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptAnswerRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class GameAssignmentService {

    private final GameAssignmentRepository assignmentRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameAttemptRepository attemptRepository;
    private final GameAttemptAnswerRepository answerRepository;
    private final GameQuestionRepository questionRepository;

    public GameAssignmentService(
            GameAssignmentRepository assignmentRepository,
            GameRepository gameRepository,
            UserRepository userRepository,
            GameAttemptRepository attemptRepository,
            GameAttemptAnswerRepository answerRepository,
            GameQuestionRepository questionRepository) {

        this.assignmentRepository = assignmentRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    // Assign a game to a particular grade and section
    @Transactional
    public GameAssignment assignGame(
            Long gameId,
            String teacherEmail,
            Integer grade,
            String section,
            LocalDateTime deadline) {

        // Validate teacher
        User teacher = userRepository.findByEmail(teacherEmail)
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() ->
                        new IllegalArgumentException("Teacher account not found."));

        // Validate game
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Game not found."));

        // Only preloaded games or the logged-in teacher's own games
        if (game.getSource() == GameSource.CUSTOM) {
            if (game.getCreatedBy() == null ||
                    !game.getCreatedBy().getId().equals(teacher.getId())) {
                throw new IllegalArgumentException(
                        "You can only assign your own custom games.");
            }
        } else if (game.getSource() != GameSource.PRELOADED) {
            throw new IllegalArgumentException("Invalid game source.");
        }

        if (game.getGameType() == GameType.PUZZLE
                && !PuzzleConfiguration.isSupportedGame(questionRepository
                        .findByGameIdOrderByQuestionOrderAsc(gameId))) {
            throw new IllegalArgumentException(
                    "Only puzzles using one of the four supported techniques can be assigned.");
        }

        // Validate game visibility
        if (game.getVisibility() != GameVisibility.AVAILABLE_TO_STUDENTS) {
            throw new IllegalArgumentException(
                    "This game is hidden from students and cannot be assigned.");
        }

        // Validate grade
        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException(
                    "Grade must be between 1 and 5.");
        }

        if (!game.getGrade().equals(grade)) {
            throw new IllegalArgumentException(
                    "The selected game's grade does not match the class grade.");
        }

        // Validate section
        String normalizedSection = normalizeSection(section);
        if (normalizedSection.isEmpty() || normalizedSection.length() > 20) {
            throw new IllegalArgumentException(
                    "Please enter a valid class section.");
        }

        // Validate deadline
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Assignment deadline must be in the future.");
        }

        GameAssignment assignment = new GameAssignment();
        assignment.setGame(game);
        assignment.setTeacher(teacher);
        assignment.setGrade(grade);
        assignment.setSection(normalizedSection);
        assignment.setDeadline(deadline);

        return assignmentRepository.save(assignment);
    }

    // Get all assignments created by the logged-in teacher
    @Transactional(readOnly = true)
    public List<GameAssignment> getAssignmentsForTeacher(String teacherEmail) {
        return assignmentRepository
                .findByTeacher_EmailOrderByAssignedAtDesc(teacherEmail);
    }

    // Get assignments for a particular class
    @Transactional(readOnly = true)
    public List<GameAssignment> getAssignmentsForClass(
            Integer grade, String section) {

        return assignmentRepository.findByGradeAndSectionIgnoreCaseAndActiveTrueOrderByDeadlineAsc(
                grade, normalizeSection(section));
    }

    @Transactional
    public GameAssignment deleteAssignment(Long assignmentId, String teacherEmail) {
        GameAssignment existing = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found or you do not own it."));
        User teacher = userRepository.findByEmail(
                        teacherEmail.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher account not found."));
        if (!existing.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "Assignment not found or you do not own it.");
        }

        GameAssignment assignment = assignmentRepository
                .findByIdForUpdateNative(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found or you do not own it."));
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "Assignment not found or you do not own it.");
        }

        assignment.setActive(false);
        assignmentRepository.saveAndFlush(assignment);
        List<GameAttempt> attempts = attemptRepository
                .findByAssignmentIdForUpdate(assignmentId);
        for (GameAttempt attempt : attempts) {
            answerRepository.deleteByAttempt_Id(attempt.getId());
        }
        answerRepository.flush();
        attemptRepository.deleteAll(attempts);
        attemptRepository.flush();
        assignmentRepository.delete(assignment);
        assignmentRepository.flush();
        return assignment;
    }

    private String normalizeSection(String section) {
        return section == null ? "" : section.trim().toUpperCase(Locale.ROOT);
    }
}
