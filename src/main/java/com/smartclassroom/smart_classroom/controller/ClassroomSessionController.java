
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.ClassroomSessionService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/sessions")
public class ClassroomSessionController {

    private final ClassroomSessionService sessionService;

    public ClassroomSessionController(
            ClassroomSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Display sessions belonging to the logged-in teacher
    @GetMapping
    public String showSessions(
            Model model,
            Authentication authentication) {

        String email = authentication.getName();

        model.addAttribute(
                "sessions",
                sessionService.getSessionsByTeacher(email)
        );

        return "teacher-sessions";
    }

    // Create a new session
    @PostMapping("/create")
    public String createSession(
            @RequestParam String title,
            @RequestParam Integer grade,
            @RequestParam String subject,
            @RequestParam String topic,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();

            sessionService.createSession(
                    title, grade, subject, topic, email
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Classroom session created successfully."
            );

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "error", ex.getMessage()
            );
        }

        return "redirect:/teacher/sessions";
    }

    // Start a session
    @PostMapping("/{id}/start")
    public String startSession(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();

            sessionService.startSession(id, email);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Classroom session started."
            );

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "error", ex.getMessage()
            );
        }

        return "redirect:/teacher/sessions";
    }

    // Complete a session
    @PostMapping("/{id}/complete")
    public String completeSession(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();

            sessionService.completeSession(id, email);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Classroom session completed."
            );

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "error", ex.getMessage()
            );
        }

        return "redirect:/teacher/sessions";
    }
}
