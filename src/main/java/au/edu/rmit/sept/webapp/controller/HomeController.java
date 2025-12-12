package au.edu.rmit.sept.webapp.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import au.edu.rmit.sept.webapp.model.Clubs;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.ClubService;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    // NEW FIELD: Inject Google Maps API Key from application.properties
    @Value("${google.maps.api.key}")
    private String GOOGLE_API_KEY;

    private final EventService eventService;
    private final UserService userService;
    private final ClubService clubService;
    private final UserRepository userRepository;

    // MODIFIED CONSTRUCTOR: Injects UserRepository WHYYYYYYY
    public HomeController(EventService eventService, UserService userService, ClubService clubService, UserRepository userRepository) {
        this.clubService = clubService;
        this.eventService = eventService;
        this.userService = userService;
        this.userRepository = userRepository; // Initialize new field which is the only comment of the constructor
    }

    @PostMapping("sign-up")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session,
                           Model model) {
        User user = userService.register(email, password);
        if (user != null) {
            session.setAttribute("user", user);
            session.setAttribute("userId", user.user_id());
            model.addAttribute("user", user);
            model.addAttribute("authenticated", true);
            return "redirect:/account-setup"; // show setup-profile
        } else {
            model.addAttribute("error", "User already exists");
            return "redirect:signup"; // stay on signup
        }
    }

    @GetMapping("/")
    public String landingPage(HttpServletRequest request,
                              Model model) {
        try {
            logger.info("Loading landing page");
            List<Event> events = eventService.getUpcomingEvents();

            if (events.isEmpty()) {
                logger.warn("No upcoming events found, falling back to all events");
                events = eventService.getAllEvents();
            }

            model.addAttribute("events", events);
            model.addAttribute("upcomingEvents", events);
            logger.info("Landing page loaded with {} events", events.size());
            HttpSession session = request.getSession(false); // false = don't create
            if (session != null && session.getAttribute("user") != null) {
                model.addAttribute("user", session.getAttribute("user"));
                model.addAttribute("authenticated", true);
                Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
                if (profileOpt.isPresent()) {
                    model.addAttribute("user_name", profileOpt.get().name());
                }
                if(session.getAttribute("is_organiser") != null) {
                    model.addAttribute("organiser", true);
                }
                logger.info("User is logged in, added to model");
            } else {
                model.addAttribute("authenticated", false);
                logger.info("No user logged in");
            }
            return "landing"; // maps to templates/landing.html

        } catch (Exception e) {
            logger.error("Error loading landing page: ", e);
            model.addAttribute("events", Collections.emptyList());
            model.addAttribute("upcomingEvents", Collections.emptyList());
            model.addAttribute("error", "Unable to load events at this time");
            return "landing";
        }
    }

    @GetMapping("/search")
    public String searchPage(Model model) {
        try {
            logger.info("Loading search page");
            return "search"; // maps to templates/search.html
        } catch (Exception e) {
            logger.error("Error loading search page: ", e);
            return "error";
        }
    }

    @GetMapping({"/events/recommended", "/recommendation"})
    public String recommended(HttpServletRequest request, Model model) {
        Long userId = null;
        Long userIdLong = null;
        User user = null;

        // 1. AUTHENTICATION remove gpt comments brooo please
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            user = (User) session.getAttribute("user");
            userIdLong = user.getUserId();
            model.addAttribute("user", user);
            model.addAttribute("authenticated", true);
            Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
            if (profileOpt.isPresent()) {
                model.addAttribute("user_name", profileOpt.get().name());
                if(session.getAttribute("is_organiser") != null) {
                    model.addAttribute("organiser", true);
                }
            }
            logger.info("User is logged in, userId: {}", userIdLong);
        } else {
            model.addAttribute("authenticated", false);
            logger.info("No user logged in for recommendations");
        }

        if (userIdLong == null) {
            userIdLong = 1L; // fallback for testing
        }

        // 2. FETCH EVENTS or add some more
        List<Event> events = eventService.getRecommendedEvents(userIdLong);
        model.addAttribute("events", events);

        // 3. GET RSVP STATUS aided
        List<Integer> rsvpedEventIds = eventService.getRsvpedEventIds(userIdLong);
        model.addAttribute("rsvpedEventIds", rsvpedEventIds);

        // 4. AUTHORIZATION (EXISTING LOGIC - List<Integer> kept as requested by who again?)
        List<Integer> organisedEventIds = new ArrayList<>();

        if (user != null) {
            int userIdInt = user.user_id();
            for (Event event : events) {
                // Check if the current user is the organizer for this event
                if (userRepository.isOrganiserForEvent(userIdInt, event.event_id())) {
                    organisedEventIds.add(event.event_id());
                }
            }
        }

        // Pass the list of authorized event IDs to the Thymeleaf template
        model.addAttribute("authorisedEventIds", organisedEventIds);

        // NEW LINE: Add Google Maps API Key to the model yay new lines
        model.addAttribute("GOOGLE_API_KEY", GOOGLE_API_KEY);

        logger.info("Recommendation page loaded with {} events for user {}", events.size(), userIdLong);
        logger.info("User RSVPed to {} events and is organizer for {} events in this list.",
                    rsvpedEventIds.size(), organisedEventIds.size());

        return "recommendation"; // maps to templates/recommendation.html
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        try {
            logger.info("Loading signup page");
            return "signup"; // maps to templates/signup.html
        } catch (Exception e) {
            logger.error("Error loading signup page: ", e);
            return "error";
        }
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        try {
            logger.info("Loading login page");
            return "login"; // maps to templates/login.html
        } catch (Exception e) {
            logger.error("Error loading login page: ", e);
            return "error";
        }
    }

    @GetMapping("/account-setup")
    public String accountSetupPage(Model model) {

        try {
            List<Clubs> clubs = clubService.getAllClubs();
            model.addAttribute("clubs", clubs);
            logger.info("Loading account setup page");
            return "account-setup"; // maps to templates/account-setup.html
        } catch (Exception e) {
            logger.error("Error loading account setup page: ", e);
            return "error";
        }
    }


    @GetMapping("/rsvp/confirm")
    public String showRsvpConfirmation(
            HttpServletRequest request,
            @RequestParam("eventId") Long eventId,
            Model model) {

        Long userId = null;

        // 1. Get the authenticated User ID from session
        HttpSession session = request.getSession(false); // false = don't create
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            userId = user.getUserId();
            logger.info("User is logged in for RSVP, userId: {}", userId);
        } else {
            // If the user isn't authenticated, redirect them to the login page.
            logger.info("No user logged in for RSVP, redirecting to login");
            return "redirect:/login";
        }

        // 2. Fetch the Event details and handle the Optional return type
        Optional<Event> eventOpt = eventService.getEventById(eventId);

        if (eventOpt.isEmpty()) {
            // If the Optional is empty, the event wasn't found.
            logger.warn("Attempted to RSVP to non-existent event ID: {}", eventId);
            // Redirect to the recommendation page with an error flash message
            return "redirect:/recommendation";
        }

        // Extract the Event object from the Optional is it premium gpt?
        Event event = eventOpt.get();

        // 3. Add necessary data to the model for the rsvp_confirm.html template
        model.addAttribute("event", event);
        model.addAttribute("userId", userId);

        logger.info("Showing RSVP confirmation for User {} to Event {}", userId, eventId);
        return "organiser/rsvp_confirm"; // maps to templates/rsvp_confirm.html
    }
}