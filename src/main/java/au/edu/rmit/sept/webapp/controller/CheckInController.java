package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class CheckInController {

    private final EventService eventService;
    private final UserService userService;

    public CheckInController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping("/checkin/{eventId}/{userId}")
    public String checkIn(@PathVariable("eventId") Long eventId, @PathVariable("userId") Long userId, Model model) {
        Optional<Event> eventOpt = eventService.getEventById(eventId);
        Optional<User> userOpt = userService.getUserById(userId);

        if (eventOpt.isPresent() && userOpt.isPresent()) {
            Event event = eventOpt.get();
            User user = userOpt.get();
            model.addAttribute("event", event);
            model.addAttribute("user", user);

            boolean success = eventService.checkInUser(eventId, userId);
            if (success) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Check-in successful!");
            } else {
                model.addAttribute("success", false);
                model.addAttribute("message", "Check-in failed. Invalid QR code or user not RSVP'd.");
            }
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", "Invalid Event or User.");
        }

        return "checkin-result";
    }
}