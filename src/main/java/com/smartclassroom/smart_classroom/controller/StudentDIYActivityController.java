package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.DIYActivityService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/activities")
public class StudentDIYActivityController {

    private final DIYActivityService activityService;

    public StudentDIYActivityController(DIYActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public String catalogue(Authentication authentication, Model model) {
        model.addAttribute("activities", activityService.studentCatalogue(authentication.getName()));
        return "student-diy-activities";
    }

    @GetMapping("/{activityId}")
    public String details(@PathVariable Long activityId, Authentication authentication,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("item", activityService.studentActivity(activityId, authentication.getName()));
            return "student-diy-activity-detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student/activities";
        }
    }

    @PostMapping("/{activityId}/complete")
    public String complete(@PathVariable Long activityId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            boolean added = activityService.complete(activityId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", added
                    ? "Activity completion recorded."
                    : "Your completion is already recorded.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/activities/" + activityId;
    }
}
