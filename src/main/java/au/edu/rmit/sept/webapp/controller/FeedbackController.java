package au.edu.rmit.sept.webapp.controller;

import java.security.Principal;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Event_Feedback;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);
    
    private final FeedbackService feedbackService;
    private final EventService eventService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, EventService eventService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    /**
     * Display feedback form for a specific event
     */
    @GetMapping("/event/{eventId}")
    public String showFeedbackForm(@PathVariable("eventId") int eventId, Model model, Principal principal, HttpServletRequest request) {
        try {
            // Get current user ID (with fallback for development)
            Long userId = getUserId(principal);
            
            // Handle authentication state for template
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                model.addAttribute("user", session.getAttribute("user"));
                model.addAttribute("authenticated", true);
            } else {
                model.addAttribute("authenticated", false);
            }
            
            // Get event details
            // Note: We'd need to add a findById method to EventService/Repository
            // For now, we'll get all events and find the one we need
            List<Event> allEvents = eventService.getAllEvents();
            Optional<Event> eventOpt = allEvents.stream()
                .filter(e -> e.event_id() == eventId)
                .findFirst();
            
            if (eventOpt.isEmpty()) {
                logger.warn("Event not found with ID: " + eventId);
                return "error";
            }
            
            Event event = eventOpt.get();
            
            // Check if user has already provided feedback
            Optional<Event_Feedback> existingFeedback = feedbackService.getUserEventFeedback(eventId, userId.intValue());
            
            // Add attributes to model
            model.addAttribute("event", event);
            model.addAttribute("existingFeedback", existingFeedback.orElse(null));
            model.addAttribute("hasExistingFeedback", existingFeedback.isPresent());
            
            return "feedback-form";
            
        } catch (Exception e) {
            logger.error("Error loading feedback form for event " + eventId + ": ", e);
            return "error";
        }
    }

    /**
     * Handle feedback form submission
     */
    @PostMapping("/submit")
    public String submitFeedback(
            @RequestParam("eventId") int eventId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "comments", defaultValue = "") String comments,
            RedirectAttributes redirectAttributes,
            Principal principal,
            HttpServletRequest request) {
        
        try {
            // Get current user ID (with fallback for development)
            Long userId = getUserId(principal);
            
            // Server-side validation for rating (since no JavaScript)
            if (rating == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a rating before submitting your feedback.");
                return "redirect:/feedback/event/" + eventId;
            }
            
            // Validate rating range
            if (rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid rating. Please select a rating between 1 and 5 stars.");
                return "redirect:/feedback/event/" + eventId;
            }
            
            // Validate comments length (since no JavaScript character counting)
            if (comments.length() > 500) {
                redirectAttributes.addFlashAttribute("errorMessage", "Comments cannot exceed 500 characters. Please shorten your feedback.");
                return "redirect:/feedback/event/" + eventId;
            }
            
            // Submit feedback
            feedbackService.submitFeedback(eventId, userId.intValue(), rating, comments);
            
            logger.info("Feedback submitted for event ID: " + eventId + " by user ID: " + userId + " with rating: " + rating);
            
            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback! Your rating and comments have been saved.");
            
            // Redirect back to the form to show the submitted feedback
            return "redirect:/feedback/event/" + eventId;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid feedback submission: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/feedback/event/" + eventId;
            
        } catch (Exception e) {
            logger.error("Error submitting feedback for event " + eventId + ": ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while submitting your feedback. Please try again.");
            return "redirect:/feedback/event/" + eventId;
        }
    }

    /**
     * View all feedback for an event (optional feature)
     */
    @GetMapping("/event/{eventId}/all")
    public String viewEventFeedback(@PathVariable("eventId") int eventId, Model model, HttpServletRequest request) {
        try {
            // Handle authentication state for template
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                model.addAttribute("user", session.getAttribute("user"));
                model.addAttribute("authenticated", true);
            } else {
                model.addAttribute("authenticated", false);
            }
            
            // Get event details
            List<Event> allEvents = eventService.getAllEvents();
            Optional<Event> eventOpt = allEvents.stream()
                .filter(e -> e.event_id() == eventId)
                .findFirst();
            
            if (eventOpt.isEmpty()) {
                logger.warn("Event not found with ID: " + eventId);
                return "error";
            }
            
            Event event = eventOpt.get();
            List<Event_Feedback> feedbacks = feedbackService.getEventFeedback(eventId);
            double averageRating = feedbackService.getAverageRating(eventId);
            
            model.addAttribute("event", event);
            model.addAttribute("feedbacks", feedbacks);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("totalFeedbacks", feedbacks.size());
            
            return "event-feedback-all";
            
        } catch (Exception e) {
            logger.error("Error loading feedback for event " + eventId + ": ", e);
            return "error";
        }
    }

    /**
     * Helper method to get user ID with fallback for development
     */
    private Long getUserId(Principal principal) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName())
                    .map(user -> user.getUserId())
                    .orElse(1L); // Fallback to user ID 1 if not found
        }
        return 1L; // TODO: Remove after login is fully implemented
    }
}