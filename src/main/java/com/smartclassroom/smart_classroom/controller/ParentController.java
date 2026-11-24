
package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.ParentPerformanceService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/parent")
public class ParentController {

    private final ParentPerformanceService performanceService;

    public ParentController(ParentPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    // Parent Dashboard
    @GetMapping("/dashboard")
    public String parentDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long childId,
            Model model) {
        try {
            ParentPerformanceService.ParentDashboard dashboard =
                    performanceService.getDashboard(authentication.getName(), childId);
            model.addAttribute("children", dashboard.children());
            model.addAttribute("selectedPerformance", dashboard.selectedChild());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            ParentPerformanceService.ParentDashboard dashboard =
                    performanceService.getDashboard(authentication.getName(), null);
            model.addAttribute("children", dashboard.children());
            model.addAttribute("selectedPerformance", null);
        }
        return "parent-dashboard";
    }

    // View selected child's progress
    @GetMapping("/child/{childId}/progress")
    public String childProgress(
            @PathVariable Long childId,
            Authentication authentication,
            Model model) {

        try {
            model.addAttribute("report", performanceService.getChildPerformance(
                    authentication.getName(), childId));
            return "parent-child-progress";
        } catch (IllegalArgumentException e) {
            return "redirect:/parent/dashboard?error=unauthorized";
        }
    }
}
