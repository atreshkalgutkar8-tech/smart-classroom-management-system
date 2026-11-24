package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.service.DIYKitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/kits")
public class AdminDIYKitController {

    private final DIYKitService kitService;

    public AdminDIYKitController(DIYKitService kitService) {
        this.kitService = kitService;
    }

    @GetMapping
    public String catalogue(Model model) {
        model.addAttribute("kits", kitService.catalogue());
        return "admin-diy-kits";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name, @RequestParam String description,
            @RequestParam String items, RedirectAttributes redirectAttributes) {
        try {
            kitService.create(name, description, items);
            redirectAttributes.addFlashAttribute("success", "DIY kit added to the catalogue.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/kits";
    }
}
