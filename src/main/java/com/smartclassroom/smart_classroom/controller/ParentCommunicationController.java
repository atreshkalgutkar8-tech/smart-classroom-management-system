package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.ParentCommunicationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parent")
public class ParentCommunicationController {
    private final ParentCommunicationService communicationService;

    public ParentCommunicationController(ParentCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/conversations")
    public String conversations(Authentication authentication, Model model) {
        model.addAttribute("conversations", communicationService.getParentConversations(
                authentication.getName()));
        return "parent-conversations";
    }

    @GetMapping("/conversations/new")
    public String startConversation(@RequestParam Long studentId,
            Authentication authentication, Model model, RedirectAttributes redirect) {
        try {
            var children = communicationService.getParentChildren(authentication.getName());
            var student = children.stream().filter(child -> child.id().equals(studentId))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "That student is not linked to your parent account."));
            model.addAttribute("student", student);
            model.addAttribute("teachers", communicationService.getAvailableTeachers(
                    authentication.getName(), studentId));
            return "parent-conversation-start";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/dashboard";
        }
    }

    @PostMapping("/conversations/open")
    public String openConversation(@RequestParam Long studentId, @RequestParam Long teacherId,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            Long id = communicationService.openParentConversation(
                    authentication.getName(), studentId, teacherId);
            return "redirect:/parent/conversations/" + id;
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/conversations";
        }
    }

    @GetMapping("/conversations/{id}")
    public String conversation(@PathVariable Long id, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("conversation", communicationService.getParentConversation(
                    authentication.getName(), id));
            return "parent-conversation";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/conversations";
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public String sendMessage(@PathVariable Long id, @RequestParam String content,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            communicationService.sendParentMessage(authentication.getName(), id, content);
            redirect.addFlashAttribute("successMessage", "Message sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/parent/conversations/" + id;
    }

    @GetMapping("/complaints")
    public String complaints(Authentication authentication, Model model) {
        model.addAttribute("complaints", communicationService.getParentComplaints(
                authentication.getName()));
        return "parent-complaints";
    }

    @GetMapping("/complaints/new")
    public String newComplaint(@RequestParam(required = false) Long studentId,
            Authentication authentication, Model model, RedirectAttributes redirect) {
        try {
            var children = communicationService.getParentChildren(authentication.getName());
            model.addAttribute("children", children);
            model.addAttribute("selectedStudentId", studentId);
            model.addAttribute("teachers", studentId == null ? java.util.List.of()
                    : communicationService.getAvailableTeachers(authentication.getName(), studentId));
            return "parent-complaint-new";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/dashboard";
        }
    }

    @PostMapping("/complaints")
    public String createComplaint(@RequestParam Long studentId, @RequestParam Long teacherId,
            @RequestParam String title, @RequestParam String description,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            Long id = communicationService.createComplaint(authentication.getName(),
                    studentId, teacherId, title, description);
            redirect.addFlashAttribute("successMessage", "Your concern was submitted.");
            return "redirect:/parent/complaints/" + id;
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/complaints/new?studentId=" + studentId;
        }
    }

    @GetMapping("/complaints/{id}")
    public String complaint(@PathVariable Long id, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("complaint", communicationService.getParentComplaint(
                    authentication.getName(), id));
            return "parent-complaint-detail";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/parent/complaints";
        }
    }

    @PostMapping("/complaints/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String content,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            communicationService.replyToComplaintAsParent(authentication.getName(), id, content);
            redirect.addFlashAttribute("successMessage", "Reply sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/parent/complaints/" + id;
    }
}
