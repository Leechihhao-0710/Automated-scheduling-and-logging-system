package com.example.demo.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Employee;

@Controller
public class dashboardController {

    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    // @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/dashboard")
    public String userDashboard(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "dashboard");
        return "user/user_dashboard";
    }

    // // @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/current-tasks")
    public String userCurrentTask(@AuthenticationPrincipal Employee employee,
            Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "current-tasks");
        return "user/current_tasks";
    }

    @GetMapping("/user/user-task-management")
    public String userTaskManagement(@AuthenticationPrincipal Employee employee,
            Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "user-task-management");
        return "user/user_task_management";
    }

    @GetMapping("/admin/overview")
    public String adminOverview(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "overview");
        return "admin/admin_overview";
    }

    @GetMapping("/user/overview")
    public String userOverview(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "overview");
        return "user/user_overview";
    }

}