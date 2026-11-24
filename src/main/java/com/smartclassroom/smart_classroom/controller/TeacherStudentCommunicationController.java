package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.StudentCommunicationService;
import com.smartclassroom.smart_classroom.service.TeacherStudentService;
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
@RequestMapping("/teacher/students/communication")
public class TeacherStudentCommunicationController {

    private final StudentCommunicationService communicationService;
    private final TeacherStudentService teacherStudentService;

    public TeacherStudentCommunicationController(StudentCommunicationService communicationService,
            TeacherStudentService teacherStudentService) {
        this.communicationService = communicationService;
        this.teacherStudentService = teacherStudentService;
    }

    @GetMapping
    public String inbox(Authentication authentication, Model model) {
        model.addAttribute("conversations", communicationService.getTeacherConversations());
        model.addAttribute("students", teacherStudentService
                .getStudents(authentication.getName(), null, null, null).students());
        model.addAttribute("unreadCount", communicationService.countUnreadForTeacher());
        return "teacher-student-communications";
    }

    @PostMapping("/open")
    public String openConversation(@RequestParam Long studentId, RedirectAttributes redirect) {
        try {
            Long conversationId = communicationService.openOrCreateTeacherConversation(studentId);
            return "redirect:/teacher/students/communication/" + conversationId;
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/students/communication";
        }
    }

    @GetMapping("/{conversationId}")
    public String conversation(@PathVariable Long conversationId, Model model,
            RedirectAttributes redirect) {
        try {
            model.addAttribute("conversation",
                    communicationService.getTeacherConversation(conversationId));
            model.addAttribute("unreadCount", communicationService.countUnreadForTeacher());
            return "teacher-student-conversation";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/students/communication";
        }
    }

    @PostMapping("/{conversationId}/messages")
    public String sendMessage(@PathVariable Long conversationId, @RequestParam String content,
            RedirectAttributes redirect) {
        try {
            communicationService.sendTeacherMessage(conversationId, content);
            redirect.addFlashAttribute("successMessage", "Message sent.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/teacher/students/communication/" + conversationId;
    }
}
