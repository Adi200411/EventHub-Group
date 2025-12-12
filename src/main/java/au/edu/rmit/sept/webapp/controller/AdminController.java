package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.AdminService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/events")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
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

    //  View all events (Admin only)
    @GetMapping
    public String listAllEvents(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested to view all events");
        List<Event> events = adminService.getAllEvents();
        model.addAttribute("events", events);
        return "admin/events";
    }

    //  View event detail (Admin only)
    @GetMapping("/{id}")
    public String viewEvent(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested details for event ID: {}", id);
        Optional<Event> event = adminService.getEventById(id);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error";
        }
        model.addAttribute("event", event.get());
        return "admin/event_detail";
    }

    //  Edit event form (Admin only)
    @GetMapping("/{id}/edit")
    public String editEventForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested edit form for event ID: {}", id);
        Optional<Event> event = adminService.getEventById(id);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error";
        }
        model.addAttribute("event", event.get());
        return "admin/Admin_event_form";
    }

 // ✅ Save event edit (Admin only)
@PostMapping("/{id}/edit")
public String saveEventEdit(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam String description,
                            @RequestParam String location,
                            @RequestParam String status,
                            @RequestParam int capacity,
                            @RequestParam(required = false) String date,
                            @RequestParam(required = false) String startTime,
                            @RequestParam(required = false) String finishTime,
                            HttpSession session,
                            Model model) {
    if (!isAdmin(session)) {
        return "redirect:/access-denied";
    }

    logger.info("Admin saving edits for event ID: {}", id);

    Event existing = adminService.getEventById(id).orElseThrow();

    try {
        // ✅ Parse event date safely
        LocalDate eventDate = (date != null && !date.isEmpty())
                ? LocalDate.parse(date)
                : existing.date();

        // ✅ Parse start time safely
        LocalTime start = (startTime != null && !startTime.isEmpty())
                ? LocalTime.parse(startTime)
                : existing.start_time().toLocalTime();

        // ✅ Parse finish time safely (store only "HH:mm" string in DB)
        String finishTimeStr;
        if (finishTime != null && !finishTime.isEmpty()) {
            LocalTime end = LocalTime.parse(finishTime);
            finishTimeStr = end.toString(); // ✅ saves as "12:30"
        } else {
            finishTimeStr = existing.finish_time();
        }

        // ✅ Combine date + start time into LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.of(eventDate, start);

        // ✅ Build updated Event object
        Event updated = new Event(
                existing.event_id(),
                existing.organiser_id(),
                existing.club_id(),
                title,
                description,
                location,
                eventDate,
                startDateTime,
                finishTimeStr, // ✅ stays as String
                status,
                capacity
        );
        adminService.updateEvent(updated);
        logger.info("✅ Event {} updated successfully", id);

    } catch (Exception e) {
        logger.error("❌ Failed to parse date/time for event edit", e);
        model.addAttribute("error", "Invalid date or time format.");
        return "error";
    }

    return "redirect:/admin/events";
}



    //  Delete event (Admin only)
    @PostMapping("/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @RequestParam String reason,
                              HttpSession session,
                              Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        logger.info("Admin deleting event ID: {} with reason: {}", id, reason);

        //  Use session userId instead of hardcoded 1L
        Number adminIdNum = (Number) session.getAttribute("userId");
        Long adminId = adminIdNum != null ? adminIdNum.longValue() : null;
        if (adminId == null) {
            return "redirect:/access-denied";
        }
        int result = adminService.deleteEventById(id, adminId, reason);
        if (result == 0) {
            addAdminAttributesToModel(session, model);
            model.addAttribute("error", "Failed to delete event");
            return "error";
        }
        return "redirect:/admin/events/deleted";
    }

    //  View deleted events (Admin only)
    @GetMapping("/deleted")
    public String viewDeletedEvents(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }
        addAdminAttributesToModel(session, model);
        logger.info("Admin requested list of deleted events");
        List<Event> deletedEvents = adminService.getDeletedEvents();
        List<DeletedEventLog> logs = adminService.getDeletedEventLogs();
        model.addAttribute("events", deletedEvents);
        model.addAttribute("logs", logs);
        return "admin/deleted_events";
    }
}