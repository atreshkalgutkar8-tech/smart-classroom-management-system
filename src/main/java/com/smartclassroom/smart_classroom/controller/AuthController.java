
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Display registration page
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // Register a new user
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            Model model) {

        try {
            // Prevent public registration as Admin
            if (role == Role.ADMIN) {
                model.addAttribute(
                        "error",
                        "Admin accounts cannot be created through public registration."
                );
                return "register";
            }

            userService.registerUser(
                    fullName,
                    email,
                    password,
                    role
            );

            model.addAttribute(
                    "success",
                    "Account created successfully! You can now log in."
            );

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return "register";
    }

    // Display custom login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
}
