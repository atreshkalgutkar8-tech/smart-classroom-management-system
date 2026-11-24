
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.StudentDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final StudentDashboardService studentDashboardService;

    public DashboardController(StudentDashboardService studentDashboardService) {
        this.studentDashboardService = studentDashboardService;
    }

    // Redirect users to their dashboard based on their role
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {

        for (GrantedAuthority authority :
                authentication.getAuthorities()) {

            String role = authority.getAuthority();

            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            }

            if (role.equals("ROLE_TEACHER")) {
                return "redirect:/teacher/dashboard";
            }

            if (role.equals("ROLE_PARENT")) {
                return "redirect:/parent/dashboard";
            }

            if (role.equals("ROLE_STUDENT")) {
                return "redirect:/student/dashboard";
            }
        }

        return "redirect:/login?error";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard() {
        return "teacher-dashboard";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", studentDashboardService.getDashboard(authentication.getName()));
        return "student-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }
}
