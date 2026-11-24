package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.ComplaintStatus;
import com.smartclassroom.smart_classroom.service.ParentCommunicationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/parents")
public class TeacherParentController {
    private final ParentCommunicationService communicationService;

    public TeacherParentController(ParentCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping
    public String directory(@RequestParam(required = false) String studentName,
            @RequestParam(required = false) String parentName,
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) String section,
            Authentication authentication, Model model) {
        model.addAttribute("directory", communicationService.getTeacherParentDirectory(
                authentication.getName(), studentName, parentName, grade, section));
        model.addAttribute("studentName", value(studentName));
        model.addAttribute("parentName", value(parentName));
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedSection", value(section));
        model.addAttribute("complaintStatuses", ComplaintStatus.values());
        return "teacher-parents";
    }

    @GetMapping("/conversations")
    public String conversations(Authentication authentication, Model model) {
        model.addAttribute("conversations", communicationService.getTeacherConversations(
                authentication.getName()));
        return "teacher-parent-conversations";
    }

    @PostMapping("/conversations/open")
    public String openConversation(@RequestParam Long studentId, @RequestParam Long parentId,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            Long id = communicationService.openTeacherConversation(
                    authentication.getName(), studentId, parentId);
            return "redirect:/teacher/parents/conversations/" + id;
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/parents";
        }
    }

    @GetMapping("/conversations/{id}")
    public String conversation(@PathVariable Long id, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("conversation", communicationService.getTeacherConversation(
                    authentication.getName(), id));
            return "teacher-parent-conversation";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/parents/conversations";
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public String sendMessage(@PathVariable Long id, @RequestParam String content,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            communicationService.sendTeacherMessage(authentication.getName(), id, content);
            redirect.addFlashAttribute("successMessage", "Message sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/teacher/parents/conversations/" + id;
    }

    @GetMapping("/complaints")
    public String complaints(Authentication authentication, Model model) {
        model.addAttribute("complaints", communicationService.getTeacherComplaints(
                authentication.getName()));
        return "teacher-complaints";
    }

    @GetMapping("/complaints/{id}")
    public String complaint(@PathVariable Long id, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("complaint", communicationService.getTeacherComplaint(
                    authentication.getName(), id));
            model.addAttribute("statuses", ComplaintStatus.values());
            return "teacher-complaint-detail";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/parents/complaints";
        }
    }

    @PostMapping("/complaints/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String content,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            communicationService.replyToComplaintAsTeacher(authentication.getName(), id, content);
            redirect.addFlashAttribute("successMessage", "Reply sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/teacher/parents/complaints/" + id;
    }

    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam ComplaintStatus status,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            communicationService.updateComplaintStatus(authentication.getName(), id, status);
            redirect.addFlashAttribute("successMessage", "Complaint status updated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/teacher/parents/complaints/" + id;
    }

    private static String value(String value) { return value == null ? "" : value; }
}
