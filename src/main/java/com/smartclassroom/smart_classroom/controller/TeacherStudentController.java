package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.TeacherStudentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/students")
public class TeacherStudentController {

    private final TeacherStudentService studentService;

    public TeacherStudentController(TeacherStudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public String students(@RequestParam(required = false) Integer grade,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String view,
            Authentication authentication, Model model) {
        try {
            var result = studentService.getStudents(authentication.getName(), grade, section, name);
            model.addAttribute("students", result.students());
            model.addAttribute("grades", result.grades());
            model.addAttribute("sections", result.sections());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("students", java.util.List.of());
            model.addAttribute("grades", java.util.List.of());
            model.addAttribute("sections", java.util.List.of());
            model.addAttribute("errorMessage", exception.getMessage());
        }
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedSection", section == null ? "" : section);
        model.addAttribute("nameFilter", name == null ? "" : name);
        model.addAttribute("performanceView", "performance".equals(view));
        return "teacher-students";
    }

    @GetMapping("/{studentId}")
    public String studentDetails(@PathVariable Long studentId,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("detail", studentService.getStudent(
                    authentication.getName(), studentId));
            return "teacher-student-detail";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/teacher/students";
        }
    }
}
