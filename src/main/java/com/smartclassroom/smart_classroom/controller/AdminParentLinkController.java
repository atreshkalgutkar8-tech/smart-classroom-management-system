
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminParentLinkController {

    private final UserService userService;

    public AdminParentLinkController(UserService userService) {
        this.userService = userService;
    }

    // Display parent-student linking page
    @GetMapping("/parent-student-linking")
    public String showLinkingPage(
            @RequestParam(required = false) String parentKeyword,
            @RequestParam(required = false) String studentKeyword,
            Model model) {

        List<User> parents =
                userService.searchParents(parentKeyword);

        List<User> students =
                userService.searchStudents(studentKeyword);

        model.addAttribute("parents", parents);
        model.addAttribute("students", students);

        model.addAttribute("parentKeyword",
                parentKeyword == null ? "" : parentKeyword);

        model.addAttribute("studentKeyword",
                studentKeyword == null ? "" : studentKeyword);

        return "admin-parent-linking";
    }

    // Link selected student to selected parent
    @PostMapping("/parent-student-linking/link")
    public String linkStudent(
            @RequestParam Long parentId,
            @RequestParam Long studentId,
            RedirectAttributes redirectAttributes) {

        try {

            userService.linkStudentToParent(parentId, studentId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Student successfully linked to parent!");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/admin/parent-student-linking";
    }
}
