package com.milind.taskportal.controller;

import com.milind.taskportal.model.Task;
import com.milind.taskportal.model.User;
import com.milind.taskportal.service.TaskService;
import com.milind.taskportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final UserService userService;

    @GetMapping("/new")
    public String newTaskForm(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("statuses", Task.Status.values());
        model.addAttribute("priorities", Task.Priority.values());
        return "tasks/form";
    }

    @PostMapping("/save")
    public String saveTask(@ModelAttribute Task task,
                           @RequestParam(required = false) Long assignedToId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        User creator = userService.findByUsername(userDetails.getUsername());
        task.setCreatedBy(creator);
        if (assignedToId != null) {
            userService.findAll().stream()
                    .filter(u -> u.getId().equals(assignedToId))
                    .findFirst().ifPresent(task::setAssignedTo);
        }
        taskService.save(task);
        ra.addFlashAttribute("success", "Task saved successfully!");
        return "redirect:/dashboard";
    }

    @GetMapping("/edit/{id}")
    public String editTask(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskService.findById(id));
        model.addAttribute("users", userService.findAll());
        model.addAttribute("statuses", Task.Status.values());
        model.addAttribute("priorities", Task.Priority.values());
        return "tasks/form";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam Task.Status status) {
        taskService.updateStatus(id, status);
        return "redirect:/dashboard";
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id, RedirectAttributes ra) {
        taskService.delete(id);
        ra.addFlashAttribute("success", "Task deleted.");
        return "redirect:/dashboard";
    }
}
