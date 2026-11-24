package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.ClassroomResourceType;
import com.smartclassroom.smart_classroom.service.ClassroomPresentationService;
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
@RequestMapping("/teacher/sessions")
public class ClassroomPresentationController {

    private final ClassroomPresentationService presentationService;

    public ClassroomPresentationController(ClassroomPresentationService presentationService) {
        this.presentationService = presentationService;
    }

    @GetMapping("/{sessionId}/resources")
    public String resourceManager(@PathVariable Long sessionId, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("manager", presentationService.getResourceManager(
                    sessionId, authentication.getName()));
            model.addAttribute("resourceTypes", ClassroomResourceType.values());
            return "teacher-session-resources";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/teacher/sessions";
        }
    }

    @PostMapping("/{sessionId}/resources")
    public String addResource(@PathVariable Long sessionId,
            @RequestParam String title,
            @RequestParam ClassroomResourceType resourceType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String resourceUrl,
            @RequestParam(required = false) String contentText,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            presentationService.addResource(sessionId, authentication.getName(), title,
                    resourceType, description, resourceUrl, contentText);
            redirect.addFlashAttribute("success", "Learning resource added to this session.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/resources";
    }

    @PostMapping("/{sessionId}/resources/{resourceId}/delete")
    public String deleteResource(@PathVariable Long sessionId, @PathVariable Long resourceId,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            presentationService.deleteResource(sessionId, resourceId, authentication.getName());
            redirect.addFlashAttribute("success", "Resource removed from this session.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/resources";
    }

    @GetMapping("/{sessionId}/presentation")
    public String presentation(@PathVariable Long sessionId, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("presentation", presentationService.getPresentation(
                    sessionId, authentication.getName()));
            return "classroom-presentation";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/teacher/sessions";
        }
    }

    @PostMapping("/{sessionId}/presentation/start")
    public String startPresentation(@PathVariable Long sessionId, Authentication authentication,
            RedirectAttributes redirect) {
        try {
            presentationService.startPresentation(sessionId, authentication.getName());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/presentation";
    }

    @PostMapping("/{sessionId}/presentation/next")
    public String nextResource(@PathVariable Long sessionId, Authentication authentication,
            RedirectAttributes redirect) {
        try {
            presentationService.nextResource(sessionId, authentication.getName());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/presentation";
    }

    @PostMapping("/{sessionId}/presentation/end")
    public String endPresentation(@PathVariable Long sessionId, Authentication authentication,
            RedirectAttributes redirect) {
        try {
            presentationService.endPresentation(sessionId, authentication.getName());
            redirect.addFlashAttribute("success", "Presentation ended. The classroom session remains unchanged.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teacher/sessions/" + sessionId + "/presentation";
    }
}
