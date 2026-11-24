package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.ClassroomSessionParticipationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/sessions/{sessionId}/participation")
public class ClassroomSessionParticipationController {

    private final ClassroomSessionParticipationService participationService;

    public ClassroomSessionParticipationController(ClassroomSessionParticipationService participationService) {
        this.participationService = participationService;
    }

    @GetMapping
    public String showParticipation(@PathVariable Long sessionId, Authentication authentication,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("page", participationService.getPage(sessionId, authentication.getName()));
            return "teacher-session-participation";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/sessions";
        }
    }

    @PostMapping
    public String recordParticipation(@PathVariable Long sessionId,
            @RequestParam(required = false) java.util.List<Long> studentIds,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            int added = participationService.recordParticipants(sessionId, authentication.getName(), studentIds);
            redirectAttributes.addFlashAttribute("success", added == 0
                    ? "No new participation records were added. Existing records are preserved."
                    : added + " student participation record(s) saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/participation";
    }
}
