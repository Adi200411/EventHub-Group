package au.edu.rmit.sept.webapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Event_Photos;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.PhotoService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.HttpSession;

@Controller
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final EventService eventService;
    private final UserService userService;
    private final PhotoService photoService;

    public BookingController(EventService eventService, PhotoService photoService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
        this.photoService = photoService;
    }

    @GetMapping("/my-bookings")
    public String myBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Event> bookedEvents = eventService.getRsvpedEvents(user.getUserId());
        List<Event> pastAttendedEvents = eventService.getPastEventsAttendedByUser(user.getUserId());
        
        // Create thumbnails map for past events
        Map<Integer, List<Event_Photos>> eventThumbnails = new HashMap<>();
        for (Event event : pastAttendedEvents) {
            List<Event_Photos> thumbnails = photoService.getEventPhotoThumbnails(event.event_id(), 3);
            eventThumbnails.put(event.event_id(), thumbnails);
        }
        
        model.addAttribute("bookedEvents", bookedEvents);
        model.addAttribute("pastAttendedEvents", pastAttendedEvents);
        model.addAttribute("eventThumbnails", eventThumbnails);
        model.addAttribute("user", user);
        model.addAttribute("authenticated", true);
        Optional<Student_Profile> profileOpt = userService.getProfileByUserId(user.user_id());
        if (profileOpt.isPresent()) {
            model.addAttribute("user_name", profileOpt.get().name());
        }

        return "my-bookings";
    }
    @PostMapping("/cancel-booking")
    public String cancelBooking(@RequestParam("eventId") Long eventId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        eventService.cancelRsvp(eventId, user.getUserId());
        return "redirect:/my-bookings";
    }
}