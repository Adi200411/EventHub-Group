package au.edu.rmit.sept.webapp.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.ClubService;
import au.edu.rmit.sept.webapp.service.OrganiserService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final OrganiserService organiserService;
    private final ClubService clubService;

    public UserController(UserService userService, OrganiserService organiserService, ClubService clubService) {
        this.userService = userService;
        this.organiserService = organiserService;
        this.clubService = clubService;
    }

    // GET all users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // GET user by ID
    @GetMapping("/{id}")
    public Optional<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }


    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        User user = userService.findByUsernameAndPassword(email, password);

        if (user != null) {
            // Check if the user's account is active
            if ("BANNED".equalsIgnoreCase(user.status())) {
                String errorMessage = "Your account has been banned. Please contact an administrator.";
                logger.warn("Login failed for user {}: Account status is {}", email, user.status());
                model.addAttribute("error", errorMessage);
                model.addAttribute("authenticated", false);
                return "login"; // Return to login page with an error
            }
            if (!"ACTIVE".equalsIgnoreCase(user.status())) {
                String errorMessage = "Your account has been " + user.status().toLowerCase() + ". Please contact an administrator.";
                logger.warn("Login failed for user {}: Account status is {}", email, user.status());
                model.addAttribute("error", errorMessage);
                model.addAttribute("authenticated", false);
                return "login"; // Return to login page with an error
            }

            // create session if it doesn't exist
            HttpSession session = request.getSession(true); // true = create if doesn't exist
            session.setAttribute("user", user);            // store the user object
            session.setAttribute("userId", user.user_id());
            Optional<Organiser_Profile> organiserOpt = userService.findOrganiserById(user.user_id());
            logger.info("User id found: {}", user.user_id());
            if (organiserOpt.isPresent()) {
                logger.info("Organiser id: {}", organiserOpt.get().organiser_id());
                session.setAttribute("organiser", organiserOpt.get());
                session.setAttribute("is_organiser", true);
                session.setAttribute("organiserId", organiserOpt.get().organiser_id());
                model.addAttribute("is_organiser", true);
            }
            //  Fetch and store role
            String role = userService.getUserRoleById(user.user_id());
            session.setAttribute("role", role);

            model.addAttribute("user", user);
            model.addAttribute("authenticated", true);
            logger.info("user logged in successfully");

            return "redirect:/"; // back to landing
        } else {
            logger.info("user typed not found");
            model.addAttribute("error", "Invalid email or password");
            model.addAttribute("authenticated", false);
            return "login"; // stay on login page
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // Clear the JSESSIONID cookie on the client
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // expire immediately
        response.addCookie(cookie);

        return "redirect:/"; // back to landing page
    }

    @PostMapping("/register")
    public String setupAccount(@RequestParam String name,
                               @RequestParam String course,
                               @RequestParam String interest,
                               @RequestParam String role,
                               @RequestParam(required = false) String roleTitle,
                               @RequestParam(required = false) Integer clubId,
                               HttpSession session,
                               Model model) {
        Integer id = (Integer) session.getAttribute("userId");
        userService.saveUser(id, name, course, interest);

        if("organiser".equalsIgnoreCase(role)) {
            session.setAttribute("is_organiser", true);
            organiserService.saveOrganiser(id, clubId, roleTitle);
            userService.assignUserRole(id, 2);
            session.setAttribute("role", "ORGANISER");

        } else {
            userService.assignUserRole(id, 1);
            session.setAttribute("role", "STUDENT");
        }


        return "redirect:/";
    }

    @PostMapping("/update")
    public String updateAccount(@RequestParam int id,
                                @RequestParam String name,
                                @RequestParam String course,
                                @RequestParam String interest,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Model model, HttpSession session, RedirectAttributes redirectAttrs) {

        // Merge profile fields: if a field is blank, keep existing value
        Optional<Student_Profile> profileOpt = userService.getProfileByUserId(id);

        String finalName = name;
        String finalCourse = course;
        String finalInterest = interest;

        if (profileOpt.isPresent()) {
            Student_Profile existing = profileOpt.get();
            if (finalName == null || finalName.isBlank()) finalName = existing.name();
            if (finalCourse == null || finalCourse.isBlank()) finalCourse = existing.course();
            if (finalInterest == null || finalInterest.isBlank()) finalInterest = existing.interest();

            userService.updateUser(id, finalName, finalCourse, finalInterest);
        } else {
            // No existing profile: only save when at least one non-blank field provided
            boolean hasAny = (finalName != null && !finalName.isBlank()) || (finalCourse != null && !finalCourse.isBlank()) || (finalInterest != null && !finalInterest.isBlank());
            if (hasAny) {
                userService.saveUser(id, finalName, finalCourse, finalInterest);
            }
        }

        // Handle password change if requested
        if (newPassword != null && !newPassword.isBlank()) {
            // Basic confirmation check
            if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "New passwords do not match");
                return "edit-profile";
            }

            // Verify current password
            Optional<User> userOpt = userService.getUserById((long) id);
            if (userOpt.isEmpty()) {
                model.addAttribute("error", "User not found");
                return "edit-profile";
            }
            User user = userOpt.get();

            // Verify current password via service
            User auth = userService.findByUsernameAndPassword(user.email(), currentPassword == null ? "" : currentPassword);
            if (auth == null) {
                model.addAttribute("error", "Current password is incorrect");
                return "edit-profile";
            }

            // If verified, update password
            userService.updatePassword(id, newPassword);

            // If the session user is the same, refresh session attribute
            HttpSession sess = session;
            if (sess != null) {
                sess.setAttribute("user", userService.getUserById((long) id).orElse(user));
            }
        }

        redirectAttrs.addFlashAttribute("successMessage", "Profile updated successfully");
        return "redirect:/";
    }

    // PUT endpoint for API/test usage
    @PutMapping("/update")
    @ResponseBody
    public String updateUser(@RequestParam int id,
                            @RequestParam String name,
                            @RequestParam String course,
                            @RequestParam String interest) {
        userService.updateUser(id, name, course, interest);
        return "account-setup";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("userId");
        Optional<Student_Profile> profileOpt = userService.getProfileByUserId(id);
        if(profileOpt.isPresent()) {
            session.setAttribute("profile", profileOpt.get());
            model.addAttribute("profile", profileOpt.get());
            model.addAttribute("user_name", profileOpt.get().name());
        }
        if (id == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.getUserById((long) id);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("authenticated", true);
        // If there's an organiser record in session, expose it and available clubs to the template
        Object organiserObj = session.getAttribute("organiser");
        if (organiserObj != null) {
            model.addAttribute("organiser", organiserObj);
        }

        try {
            model.addAttribute("clubs", clubService.getAllClubs());
        } catch (Exception e) {
            // ignore - clubs are optional for non-organiser users
        }
        return "edit-profile";
    }

    @DeleteMapping("delete")
    @ResponseBody
    public String deleteAccount(@RequestParam int id) {
        userService.deleteProfile(id);
        return "signup";
    }

}