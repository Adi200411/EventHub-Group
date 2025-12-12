package au.edu.rmit.sept.webapp.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.dto.RsvpUserDetail;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Event_Photos;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.NotificationService;
import au.edu.rmit.sept.webapp.service.PhotoService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private final EventService eventService;
    private final UserRepository userRepository;
    private final PhotoService photoService;
    private final UserService userService;
    private final NotificationService notificationService;

    public EventController(UserService userService, EventService eventService, UserRepository userRepository, 
                          PhotoService photoService, NotificationService notificationService) {
        this.userService = userService;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.photoService = photoService;
        this.notificationService = notificationService;
    }

    @PostMapping("/rsvp")
    public String rsvpForEvent(@RequestParam("eventId") Long eventId,
                                     RedirectAttributes attributes,
                                     HttpServletRequest request,
                                     HttpSession session) {
        // ... (existing logic)
        User user = (User) session.getAttribute("user");
        if (user == null) { 
            logger.warn("RSVP attempted without login, redirecting to /login");
            attributes.addFlashAttribute("error", "You must be logged in to RSVP.");
            return "redirect:/login";
        }
        Long userId = (long) user.user_id();

        eventService.rsvp(eventId, userId);
        logger.info("RSVP received for event ID: " + eventId + " from user ID: " + userId);

        //Send RSVP confirmation notification and email
        try {
            Optional<Event> eventOpt = eventService.getEventById(eventId);
            if (eventOpt.isPresent()) {
                Event event = eventOpt.get();
                String userName = getUserName(user.user_id());
                
                notificationService.notifyRsvpConfirmation(
                    user.user_id(),
                    user.email(),
                    userName,
                    event.event_id(),
                    event.title(),
                    event.formattedStartDateTime(),
                    event.location()
                );
            }
        } catch (Exception e) {
            logger.error("Failed to send RSVP confirmation notification", e);
        }

        attributes.addFlashAttribute("flashMessage", "You're RSVP'd! See you there 👋!");
        String referer = request.getHeader("Referer");
        return "redirect:/my-bookings";
    }

    @PostMapping("/cancel-rsvp")
    public String cancelRsvpForEvent(@RequestParam("eventId") Long eventId,
                                     RedirectAttributes attributes,
                                     HttpServletRequest request,
                                     HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            attributes.addFlashAttribute("error", "You must be logged in to cancel your RSVP.");
            return "redirect:/login";
        }
        Long userId = (long) user.user_id();

        eventService.cancelRsvp(eventId, userId);
        logger.info("RSVP cancelled for event ID: " + eventId + " from user ID: " + userId);

        attributes.addFlashAttribute("flashMessage", "Your RSVP has been cancelled.");
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @GetMapping("/browse")
    public String browseEvents(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "date", required = false) String date,
            HttpServletRequest request,
            Model model
    ) {
        try {
            List<Event> upcomingEvents;
            List<Event> pastEvents;
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                model.addAttribute("user", user);
                model.addAttribute("authenticated", true);
                Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
                if (profileOpt.isPresent()) {
                    model.addAttribute("user_name", profileOpt.get().name());
                }
                List<Integer> rsvpedEventIds = eventService.getRsvpedEventIds(user.getUserId());
                model.addAttribute("rsvpedEventIds", rsvpedEventIds);
                logger.info("User is logged in, added to model");
            } else {
                model.addAttribute("authenticated", false);
                model.addAttribute("rsvpedEventIds", Collections.emptyList());
                logger.info("No user logged in");
            }

            if (query != null || tag != null || date != null) {
                upcomingEvents = eventService.searchEvents(query, tag, date, true);
                pastEvents = eventService.searchEvents(query, tag, date, false);
            } else {
                upcomingEvents = eventService.getUpcomingEvents();
                pastEvents = eventService.getPastEvents();
            }

            model.addAttribute("allTags", eventService.getAllTags());

            // Create thumbnails map for past events
            Map<Integer, List<Event_Photos>> eventThumbnails = new HashMap<>();
            for (Event event : pastEvents) {
                List<Event_Photos> thumbnails = photoService.getEventPhotoThumbnails(event.event_id(), 3);
                eventThumbnails.put(event.event_id(), thumbnails);
            }

            model.addAttribute("upcomingEvents", upcomingEvents);
            model.addAttribute("pastEvents", pastEvents);
            model.addAttribute("eventThumbnails", eventThumbnails);
            model.addAttribute("query", query);
            model.addAttribute("tag", tag);
            model.addAttribute("date", date);

            return "browse";
        } catch (Exception e) {
            logger.error("Error loading browse page: ", e);
            return "error";
        }
    }


    // Existing exportRsvpListToCsv Method 
    @GetMapping("/organiser/events/{eventId}/export/rsvp")
    public void exportRsvpListToCsv(
            @PathVariable("eventId") Long eventId,
            HttpServletResponse response,
            HttpSession session) throws IOException { 
        
        // 1. AUTHENTICATION CHECK
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("Unauthorized attempt to export RSVP list for event {}: User not logged in.", eventId);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to view this list.");
            return;
        }

        // 2. AUTHORIZATION CHECK 
        String userRole = (String) session.getAttribute("role");
        Long userId = (long) user.user_id();

        // Deny access if the user is NOT the event's organizer AND is NOT an admin
        if (!userRepository.isOrganiserForEvent(userId.intValue(), eventId.intValue()) && !"ADMIN".equals(userRole)) { 
            logger.warn("Forbidden attempt to export RSVP list for event {}: User ID {} is not the organiser or an admin.", eventId, userId);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied. Only the event organizer or an admin can export the RSVP list.");
            return;
        }

        // 3. Set headers for CSV download (and the rest of the method remains the same)
        String filename = "rsvp_list_event_" + eventId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            // 3. Get data from the service layer
            List<RsvpUserDetail> rsvpUsers = eventService.getRsvpedUsersForEvent(eventId);

            // 4. Write CSV Header
            writer.println("User ID,Email,Full Name,Course,RSVP Date,QR Code");

            // 5. Write data rows
            for (RsvpUserDetail rsvpUser : rsvpUsers) {
                String line = String.join(",",
                    String.valueOf(rsvpUser.userId()),
                    rsvpUser.email(),
                    rsvpUser.name() != null ? rsvpUser.name() : "N/A",
                    rsvpUser.course() != null ? rsvpUser.course() : "N/A",
                    rsvpUser.rsvpDate().toString(),
                    rsvpUser.qrCode() != null ? rsvpUser.qrCode() : "N/A"
                );
                writer.println(line);
            }
            
            writer.flush(); 
            logger.info("Successfully exported RSVP list for event ID: {} by user ID: {}", eventId, userId);

        } catch (IOException e) {
            logger.error("Error writing CSV response for event ID {}: {}", eventId, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    // (Existing /browse/filtered Method) 
    @GetMapping("/browse/filtered")
    public String browseEventsFiltered(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletRequest request,
            Model model
    ) {
        try {
            final List<Event> upcoming = eventService.getUpcomingFiltered(q, page, size);
            final List<Event> past = eventService.getPastEvents();

            // Create thumbnails map for past events
            Map<Integer, List<Event_Photos>> eventThumbnails = new HashMap<>();
            for (Event event : past) {
                List<Event_Photos> thumbnails = photoService.getEventPhotoThumbnails(event.event_id(), 3);
                eventThumbnails.put(event.event_id(), thumbnails);
            }

            // Add user session info similar to main browse method
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                model.addAttribute("user", user);
                model.addAttribute("authenticated", true);
                Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
                if (profileOpt.isPresent()) {
                    model.addAttribute("user_name", profileOpt.get().name());
                }
                List<Integer> rsvpedEventIds = eventService.getRsvpedEventIds(user.getUserId());
                model.addAttribute("rsvpedEventIds", rsvpedEventIds);
            } else {
                model.addAttribute("authenticated", false);
                model.addAttribute("rsvpedEventIds", Collections.emptyList());
            }

            model.addAttribute("upcomingEvents", upcoming);
            model.addAttribute("pastEvents", past);
            model.addAttribute("eventThumbnails", eventThumbnails);
            model.addAttribute("query", q);
            model.addAttribute("page", page);
            model.addAttribute("size", size);

            return "browse";
        } catch (Exception e) {
            logger.error("Error loading /browse/filtered", e);
            return "error";
        }
    }


 
    @GetMapping("/events/{eventId}")
    public String viewEventDetails(
        @PathVariable("eventId") Long eventId,
        Model model,
        HttpSession session) {

        logger.info("Loading details for event ID: {}", eventId);

        try {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("authenticated", true);
                Optional<Student_Profile> profileOpt = userService.getProfileByUserId(user.user_id());
                if (profileOpt.isPresent()) {
                    model.addAttribute("user_name", profileOpt.get().name());
                }
            } else {
                model.addAttribute("authenticated", false);
            }
            
            Optional<Event> eventOpt = eventService.getEventById(eventId);

            if (eventOpt.isEmpty()) {
                logger.warn("Event not found with ID: {}", eventId);
                model.addAttribute("flashMessage", "Event not found or an error occurred.");
                return "redirect:/browse";
            }

            model.addAttribute("event", eventOpt.get());
            model.addAttribute("eventTags", eventService.getTagsByEventId(eventId.intValue()));

            // Organizer Check
            boolean isOrganiser = false;
            if (user != null) {
                isOrganiser = userRepository.isOrganiserForEvent(user.user_id(), eventId.intValue());
            }

            model.addAttribute("isOrganiser", isOrganiser);
            logger.info("Event {} organizer status for user {}: {}",
                        eventId, user != null ? user.user_id() : "N/A", isOrganiser);

            return "event-details";

        } catch (Exception e) {
            logger.error("Failed to load event details for ID: {}", eventId, e);
            model.addAttribute("flashMessage", "An unexpected error occurred while loading the event.");
            return "redirect:/browse";
        }
    }

    private String getUserName(int userId) {
        // Simple fallback: use email or "Student"
        return userRepository.findById((long) userId)
            .map(u -> u.email().split("@")[0])
            .orElse("Student");
    }
}

// ApiEventController is a separate class, which is a correct pattern.


@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/events")
class ApiEventController {

    // ... (rest of ApiEventController unchanged) ...
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ApiEventController.class);

    private final au.edu.rmit.sept.webapp.service.EventService eventService;

    public ApiEventController(au.edu.rmit.sept.webapp.service.EventService eventService) {
        this.eventService = eventService;
    }
    
    // All methods that follow (getUpcomingEvents, getEventById, createEvent, updateEvent, deleteEvent)
    // must be placed within these braces.
}