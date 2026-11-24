
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.service.StudentGameService;
import com.smartclassroom.smart_classroom.service.GameAttemptService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/student/games")
public class StudentGameController {

    private final StudentGameService studentGameService;
    private final GameAttemptService gameAttemptService;

    public StudentGameController(
            StudentGameService studentGameService,
            GameAttemptService gameAttemptService) {
        this.studentGameService = studentGameService;
        this.gameAttemptService = gameAttemptService;
    }

    // Display games assigned to the logged-in student
    @GetMapping
    public String showAssignedGames(
            Authentication authentication,
            Model model) {

        try {
            List<GameAssignment> assignments =
                    studentGameService.getAssignedGames(
                            authentication.getName()
                    );

            model.addAttribute("assignments", assignments);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("assignments", List.of());
        }

        model.addAttribute("completedAttempts",
                gameAttemptService.getCompletedAttemptsForStudent(
                        authentication.getName()));

        return "student-games";
    }
}
