
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.Game;
import com.smartclassroom.smart_classroom.entity.GameAssignment;
import com.smartclassroom.smart_classroom.service.GameAssignmentService;
import com.smartclassroom.smart_classroom.service.GameService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/teacher/games/assignments")
public class TeacherGameAssignmentController {

    private final GameAssignmentService assignmentService;
    private final GameService gameService;

    public TeacherGameAssignmentController(
            GameAssignmentService assignmentService,
            GameService gameService) {

        this.assignmentService = assignmentService;
        this.gameService = gameService;
    }

    // Display assignment page
    @GetMapping
    public String showAssignmentPage(
            @RequestParam(required = false) Integer grade,
            Authentication authentication,
            Model model) {

        String teacherEmail = authentication.getName();

        // Default grade when the teacher first opens the page
        if (grade == null) {
            grade = 1;
        }

        List<Game> games =
                gameService.getGamesForTeacher(teacherEmail, grade);

        List<GameAssignment> assignments =
                assignmentService.getAssignmentsForTeacher(teacherEmail);

        model.addAttribute("games", games);
        model.addAttribute("assignments", assignments);
        model.addAttribute("selectedGrade", grade);

        return "teacher-game-assignments";
    }

    // Assign a game to a class
    @PostMapping("/create")
    public String createAssignment(
            @RequestParam Long gameId,
            @RequestParam Integer grade,
            @RequestParam String section,
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
            LocalDateTime deadline,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            assignmentService.assignGame(
                    gameId,
                    authentication.getName(),
                    grade,
                    section,
                    deadline
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Game successfully assigned to the class!"
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/teacher/games/assignments?grade=" + grade;
    }

    @PostMapping("/{assignmentId}/delete")
    public String deleteAssignment(
            @PathVariable Long assignmentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            GameAssignment deleted = assignmentService.deleteAssignment(
                    assignmentId, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assignment, attempts, and answers were permanently deleted. The game and its questions were preserved.");
            return "redirect:/teacher/games/assignments?grade="
                    + deleted.getGrade();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/games/assignments";
        }
    }
}
