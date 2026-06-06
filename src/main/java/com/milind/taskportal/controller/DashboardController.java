package com.milind.taskportal.controller;

import com.milind.taskportal.model.User;
import com.milind.taskportal.service.TaskService;
import com.milind.taskportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final TaskService taskService;
    private final UserService userService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        model.addAttribute("user", user);
        model.addAttribute("tasks", isAdmin ? taskService.findAll() : taskService.findByUser(user));
        model.addAttribute("stats", taskService.getStats());
        return "dashboard";
    }
}
