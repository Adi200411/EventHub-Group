package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.AdminUserService;
import au.edu.rmit.sept.webapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);
    private final AdminUserService adminUserService;
    private final UserService userService;

    public AdminUserController(AdminUserService adminUserService, UserService userService) {
        this.adminUserService = adminUserService;
        this.userService = userService;
    }

    // Role check for Admin-only access
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return role != null && role.equals("ADMIN");
    }

    // Adds common model attributes for authenticated admin users
    private void addAdminAttributesToModel(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        model.addAttribute("authenticated", true);
        Optional<Student_Profile> profileOpt = userService.getProfileByUserId(user.user_id());
        profileOpt.ifPresent(profile -> model.addAttribute("user_name", profile.name()));
    }

    //  View all users
    @GetMapping
    public String listAllUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested to view all users");
        List<User> users = adminUserService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    //  View user detail
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin viewing details for user ID: {}", id);
        Optional<User> user = adminUserService.getUserById(id);
        if (user.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }
        model.addAttribute("user", user.get());
        return "admin/user_detail";
    }

    // Deactivate user
    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        User adminUser = (User) session.getAttribute("user");
        if (adminUser != null && adminUser.getUserId().equals(id)) {
            logger.warn("Admin user {} attempted to deactivate themselves.", adminUser.email());
            redirectAttributes.addFlashAttribute("error", "Admins cannot deactivate their own account.");
            return "redirect:/admin/users";
        }
        logger.info("Admin deactivating user ID: {}", id);
        boolean success = adminUserService.deactivateUser(id);
        if (!success) {
            addAdminAttributesToModel(session, model);
            model.addAttribute("error", "Failed to deactivate user");
            return "error";
        }
        redirectAttributes.addFlashAttribute("success", "User has been deactivated.");
        return "redirect:/admin/users";
    }

    // Reactivate user
    @PostMapping("/{id}/reactivate")
    public String reactivateUser(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        logger.info("Admin reactivating user ID: {}", id);
        boolean success = adminUserService.reactivateUser(id);
        if (!success) {
            addAdminAttributesToModel(session, model);
            model.addAttribute("error", "Failed to reactivate user");
            return "error";
        }
        redirectAttributes.addFlashAttribute("success", "User has been reactivated.");
        return "redirect:/admin/users";
    }

    // Ban user
    @PostMapping("/{id}/ban")
    public String banUser(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        User adminUser = (User) session.getAttribute("user");
        if (adminUser != null && adminUser.getUserId().equals(id)) {
            logger.warn("Admin user {} attempted to ban themselves.", adminUser.email());
            redirectAttributes.addFlashAttribute("error", "Admins cannot ban their own account.");
            return "redirect:/admin/users";
        }
        logger.info("Admin banning user ID: {}", id);
        boolean success = adminUserService.banUser(id);
        if (!success) {
            addAdminAttributesToModel(session, model);
            model.addAttribute("error", "Failed to ban user");
            return "error";
        }
        redirectAttributes.addFlashAttribute("success", "User has been banned successfully.");
        return "redirect:/admin/users";
    }

    //  Filter users by status
    @GetMapping("/filter")
    public String listAllUsersByStatus(@RequestParam(required = false) String status,
                                       HttpSession session,
                                       Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested to view users with status: {}", status);
        List<User> users = (status == null || status.isBlank())
                ? adminUserService.getAllUsers()
                : adminUserService.getUsersByStatus(status);

        model.addAttribute("users", users);
        return "admin/users";
    }
}