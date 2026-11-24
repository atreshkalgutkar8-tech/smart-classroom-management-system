
package com.smartclassroom.smart_classroom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.repository.GameAttemptRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;

@Service
public class GameResultService {

    private final GameAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    public GameResultService(
            GameAttemptRepository attemptRepository,
            UserRepository userRepository) {

        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GameAttempt getStudentResult(
            Long attemptId, String studentEmail) {

        // Verify logged-in user is a student
        var student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        // Find the game attempt
        GameAttempt attempt = attemptRepository.findWithAssignmentAndGameById(attemptId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game attempt not found."));

        // Verify attempt ownership
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to view this result.");
        }

        // Verify game completion
        if (!Boolean.TRUE.equals(attempt.getCompleted())) {
            throw new IllegalArgumentException(
                    "Please complete the game before viewing the result.");
        }

        return attempt;
    }
}
