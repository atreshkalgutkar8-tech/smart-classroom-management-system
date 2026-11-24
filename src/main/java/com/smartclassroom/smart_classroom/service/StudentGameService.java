
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.entity.GameVisibility;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.User;

import com.smartclassroom.smart_classroom.repository.GameAssignmentRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;
import com.smartclassroom.smart_classroom.entity.GameType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class StudentGameService {

    private final GameAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final GameQuestionRepository questionRepository;

    public StudentGameService(
            GameAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            GameQuestionRepository questionRepository) {

        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    // Get assignments available to the logged-in student
    @Transactional(readOnly = true)
    public List<GameAssignment> getAssignedGames(String studentEmail) {

        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student account not found."));

        if (student.getGrade() == null ||
                student.getSection() == null ||
                student.getSection().isBlank()) {

            throw new IllegalArgumentException(
                    "Your class details have not been assigned yet.");
        }

        String normalizedSection = student.getSection().trim()
                .toUpperCase(Locale.ROOT);

        List<GameAssignment> assignments =
                assignmentRepository.findByGradeAndSectionIgnoreCaseAndActiveTrueOrderByDeadlineAsc(
                        student.getGrade(),
                        normalizedSection
                );

        // Hidden games must not appear for students,
        // even if they were assigned earlier.
        return assignments.stream()
                .filter(assignment ->
                        assignment.getGame().getVisibility()
                                == GameVisibility.AVAILABLE_TO_STUDENTS)
                .filter(assignment -> assignment.getGame().getGameType() != GameType.PUZZLE
                        || PuzzleConfiguration.isSupportedGame(questionRepository
                                .findByGameIdOrderByQuestionOrderAsc(assignment.getGame().getId())))
                .toList();
    }
}
