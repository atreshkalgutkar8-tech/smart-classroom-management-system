package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.StudentCommunicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/communication")
public class StudentCommunicationController {

    private final StudentCommunicationService communicationService;

    public StudentCommunicationController(StudentCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping
    public String inbox(Model model) {
        model.addAttribute("conversations", communicationService.getStudentConversations());
        model.addAttribute("unreadCount", communicationService.countUnreadForStudent());
        return "student-communications";
    }

    @GetMapping("/{conversationId}")
    public String conversation(@PathVariable Long conversationId, Model model,
            RedirectAttributes redirect) {
        try {
            model.addAttribute("conversation",
                    communicationService.getStudentConversation(conversationId));
            model.addAttribute("unreadCount", communicationService.countUnreadForStudent());
            return "student-conversation";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/student/communication";
        }
    }

    @PostMapping("/{conversationId}/messages")
    public String sendMessage(@PathVariable Long conversationId, @RequestParam String content,
            RedirectAttributes redirect) {
        try {
            communicationService.sendStudentMessage(conversationId, content);
            redirect.addFlashAttribute("successMessage", "Your reply was sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/communication/" + conversationId;
    }
}
