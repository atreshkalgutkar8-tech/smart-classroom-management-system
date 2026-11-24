package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.DIYActivityService;
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
@RequestMapping("/teacher/activities")
public class TeacherDIYActivityController {

    private final DIYActivityService activityService;

    public TeacherDIYActivityController(DIYActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public String catalogue(Model model) {
        model.addAttribute("activities", activityService.teacherCatalogue());
        return "teacher-diy-activities";
    }

    @PostMapping("/create")
    public String create(@RequestParam String title, @RequestParam Integer grade,
            @RequestParam String subject, @RequestParam String topic,
            @RequestParam String learningObjective, @RequestParam String requiredMaterials,
            @RequestParam String instructions, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            activityService.create(authentication.getName(), title, grade, subject, topic,
                    learningObjective, requiredMaterials, instructions);
            redirectAttributes.addFlashAttribute("success", "DIY activity created.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/activities";
    }

    @GetMapping("/{activityId}")
    public String details(@PathVariable Long activityId, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("detail", activityService.teacherActivity(activityId));
            return "teacher-diy-activity-detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/activities";
        }
    }
}
