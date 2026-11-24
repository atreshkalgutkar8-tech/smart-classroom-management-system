
package com.smartclassroom.smart_classroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.service.GameResultService;

@Controller
@RequestMapping("/student/games")
public class StudentGameResultController {

    private final GameResultService gameResultService;

    public StudentGameResultController(
            GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @GetMapping("/attempts/{attemptId}/result")
    public String showResult(
            @PathVariable Long attemptId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            GameAttempt attempt = gameResultService.getStudentResult(
                    attemptId,
                    authentication.getName()
            );

            model.addAttribute("attempt", attempt);
            model.addAttribute("assignment", attempt.getAssignment());
            model.addAttribute("game", attempt.getAssignment().getGame());

            return "student-game-result";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

            return "redirect:/student/games";
        }
    }
}
