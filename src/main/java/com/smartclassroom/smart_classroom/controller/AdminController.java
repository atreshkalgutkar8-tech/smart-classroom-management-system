
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.service.UserService;
import com.smartclassroom.smart_classroom.service.DIYActivityService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final DIYActivityService activityService;

    public AdminController(UserService userService, DIYActivityService activityService) {
        this.userService = userService;
        this.activityService = activityService;
    }

    public record AdminUserView(Long id, String fullName, String email, String role,
            Integer grade, String section) {
        static AdminUserView from(User user) {
            return new AdminUserView(user.getId(), user.getFullName(), user.getEmail(),
                    user.getRole().name(), user.getGrade(), user.getSection());
        }
    }

    @GetMapping("/users")
    public String viewUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers().stream()
                .map(AdminUserView::from).toList());
        return "admin-users";
    }

    @GetMapping("/activities")
    public String viewActivities(Model model) {
        model.addAttribute("activities", activityService.teacherCatalogue());
        return "admin-activities";
    }

    // DTO: Exposes student details without exposing password hashes
    public record StudentView(
            Long id,
            String fullName,
            String email,
            Integer grade,
            String section) {

        public static StudentView from(User student) {
            return new StudentView(
                    student.getId(),
                    student.getFullName(),
                    student.getEmail(),
                    student.getGrade(),
                    student.getSection()
            );
        }
    }

    // View all students
    @GetMapping("/students")
    public String viewStudents(Model model) {

        List<StudentView> students = userService
                .getAllStudents()
                .stream()
                .map(StudentView::from)
                .toList();

        model.addAttribute("students", students);

        return "admin-students";
    }

    // Create a student account
    @PostMapping("/students/create")
    public String createStudent(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Integer grade,
            @RequestParam String section,
            RedirectAttributes redirectAttributes) {

        try {
            userService.createStudentAccount(
                    fullName,
                    email,
                    password,
                    grade,
                    section
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Student account created successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/students";
    }

    // Update a student's grade and section
    @PostMapping("/students/{studentId}/update-class")
    public String updateStudentClass(
            @PathVariable Long studentId,
            @RequestParam Integer grade,
            @RequestParam String section,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updateStudentClassDetails(
                    studentId,
                    grade,
                    section
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Student class details updated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/students";
    }

    // Reset a student's password
    @PostMapping("/students/{studentId}/reset-password")
    public String resetStudentPassword(
            @PathVariable Long studentId,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {

        try {
            userService.resetStudentPasswordByAdmin(
                    studentId,
                    newPassword
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Student password reset successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/students";
    }
}
