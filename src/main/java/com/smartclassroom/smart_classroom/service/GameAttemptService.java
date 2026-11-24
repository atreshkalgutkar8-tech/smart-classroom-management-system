
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameAttemptService {

    private final GameAttemptRepository attemptRepository;
    private final GameAttemptAnswerRepository answerRepository;
    private final GameAssignmentRepository assignmentRepository;
    private final GameQuestionRepository questionRepository;
    private final UserRepository userRepository;

    public GameAttemptService(
            GameAttemptRepository attemptRepository,
            GameAttemptAnswerRepository answerRepository,
            GameAssignmentRepository assignmentRepository,
            GameQuestionRepository questionRepository,
            UserRepository userRepository) {

        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    // Start a new attempt or resume an unfinished attempt
    @Transactional
    public GameAttempt startOrResumeAttempt(
            Long assignmentId,
            String studentEmail) {

        // Find and validate student
        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        // Find assignment
        GameAssignment assignment = assignmentRepository
                .findByIdForUpdateNative(assignmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game assignment not found."));

        if (!assignment.isActive()) {
            throw new IllegalArgumentException(
                    "This game assignment has been removed.");
        }

        // Verify student's grade and section
        if (student.getGrade() == null || student.getSection() == null
                || !student.getGrade().equals(assignment.getGrade())
                || !student.getSection().trim().equalsIgnoreCase(
                        assignment.getSection().trim())) {

            throw new IllegalArgumentException(
                    "You are not authorized to access this game.");
        }

        // Hidden games cannot be accessed
        if (assignment.getGame().getVisibility()
                != GameVisibility.AVAILABLE_TO_STUDENTS) {

            throw new IllegalArgumentException(
                    "This game is currently unavailable.");
        }

        List<GameQuestion> gameQuestions = questionRepository
                .findByGameIdOrderByQuestionOrderAsc(assignment.getGame().getId());
        if (assignment.getGame().getGameType() == GameType.PUZZLE
                && !PuzzleConfiguration.isSupportedGame(gameQuestions)) {
            throw new IllegalArgumentException(
                    "This legacy puzzle is preserved but is no longer playable.");
        }

        // Resume an unfinished attempt if one exists
        List<GameAttempt> previousAttempts =
                attemptRepository
                        .findByAssignment_IdAndStudent_IdOrderByAttemptNumberAsc(
                                assignmentId, student.getId());

        for (GameAttempt attempt : previousAttempts) {

            if (!Boolean.TRUE.equals(attempt.getCompleted())) {
                return attempt;
            }
        }

        // Create a new attempt
        int nextAttemptNumber = previousAttempts.size() + 1;

        long totalQuestions = gameQuestions.size();

        if (totalQuestions == 0) {
            throw new IllegalArgumentException(
                    "This game does not have any questions yet.");
        }

        GameAttempt newAttempt = new GameAttempt();

        newAttempt.setAssignment(assignment);
        newAttempt.setStudent(student);
        newAttempt.setAttemptNumber(nextAttemptNumber);
        newAttempt.setScore(0);
        newAttempt.setTotalQuestions((int) totalQuestions);
        newAttempt.setCompleted(false);

        return attemptRepository.save(newAttempt);
    }

    // Get saved answers for resuming an attempt
    @Transactional(readOnly = true)
    public List<GameAttemptAnswer> getSavedAnswers(Long attemptId) {

        return answerRepository
                .findByAttempt_IdOrderByQuestion_QuestionOrderAsc(
                        attemptId);
    }

    // Get all attempts made by a student for an assignment
    @Transactional(readOnly = true)
    public List<GameAttempt> getStudentAttempts(
            Long assignmentId,
            String studentEmail) {

        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        GameAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game assignment not found."));

        if (student.getGrade() == null || student.getSection() == null
                || !student.getGrade().equals(assignment.getGrade())
                || !student.getSection().trim().equalsIgnoreCase(
                        assignment.getSection().trim())) {

            throw new IllegalArgumentException(
                    "You are not authorized to view these attempts.");
        }

        return attemptRepository
                .findByAssignment_IdAndStudent_IdOrderByAttemptNumberAsc(
                        assignmentId, student.getId());
    }

    // Keep completed results available when an assignment is hidden or class details change.
    @Transactional(readOnly = true)
    public List<GameAttempt> getCompletedAttemptsForStudent(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student account not found."));

        return attemptRepository
                .findByStudent_IdOrderByStartedAtDesc(student.getId())
                .stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCompleted()))
                .toList();
    }
}
