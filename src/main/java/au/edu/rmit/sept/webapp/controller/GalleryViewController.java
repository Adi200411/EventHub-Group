package au.edu.rmit.sept.webapp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Event_Photos;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gallery")
public class GalleryViewController {

    private static final Logger logger = LoggerFactory.getLogger(GalleryViewController.class);
    
    private final PhotoService photoService;
    private final EventService eventService;
    private final UserRepository userRepository;

    public GalleryViewController(PhotoService photoService, EventService eventService, UserRepository userRepository) {
        this.photoService = photoService;
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    @GetMapping("/event/{eventId}")
    public String viewEventGallery(@PathVariable int eventId, Model model, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            String userEmail = null;
            
            if (session != null && session.getAttribute("user") != null) {
                sessionUser = (User) session.getAttribute("user");
                userEmail = sessionUser.email();
            }
            
            logger.info("Gallery access: User {} accessing gallery for event {}", 
                       userEmail != null ? userEmail : "anonymous", eventId);
            
            List<Event_Photos> photos = photoService.getEventPhotos(eventId);
            List<Event> events = eventService.getAllEvents();
            
            model.addAttribute("photos", photos);
            model.addAttribute("events", events);
            model.addAttribute("selectedEventId", eventId);
            
            // Find selected event for display
            Optional<Event> selectedEvent = events.stream()
                .filter(event -> event.event_id() == eventId)
                .findFirst();
            selectedEvent.ifPresent(event -> model.addAttribute("selectedEvent", event));
            
            // Check if current user can upload photos to this event
            boolean canUpload = canUploadToEvent(sessionUser, eventId);
            model.addAttribute("canUpload", canUpload);
            logger.info("Gallery authorization: User {} {} upload/delete permissions for event {}", 
                       userEmail != null ? userEmail : "anonymous", 
                       canUpload ? "HAS" : "does NOT have", eventId);
            
            // Add user information for display
            if (userEmail != null) {
                model.addAttribute("currentUserEmail", userEmail);
                model.addAttribute("authenticated", true);
                model.addAttribute("user", sessionUser);
                
                // If user doesn't have permission add logging
                if (!canUpload && sessionUser != null) {
                    logger.info("User {} tried to access event {} but doesn't have permission. They should try events 1-5 instead.", userEmail, eventId);
                }
            } else {
                // User not authenticated
                model.addAttribute("authenticated", false);
            }
            
            return "gallery-view";
        } catch (Exception e) {
            logger.error("Error loading event gallery for event {}: ", eventId, e);
            model.addAttribute("errorMessage", "Failed to load event gallery");
            return "error";
        }
    }

    @GetMapping
    public String viewGallery(
            @RequestParam(value = "eventId", required = false) Integer eventId,
            Model model,
            HttpServletRequest request
    ) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            if (session != null && session.getAttribute("user") != null) {
                sessionUser = (User) session.getAttribute("user");
            }
            
            List<Event_Photos> photos;
            List<Event> events = eventService.getAllEvents();
            
            if (eventId != null) {
                photos = photoService.getEventPhotos(eventId);
                model.addAttribute("selectedEventId", eventId);
                
                // Find selected event for display
                Optional<Event> selectedEvent = events.stream()
                    .filter(event -> event.event_id() == eventId)
                    .findFirst();
                selectedEvent.ifPresent(event -> model.addAttribute("selectedEvent", event));
                
                // Check if current user can upload photos to this event
                boolean canUpload = canUploadToEvent(sessionUser, eventId);
                model.addAttribute("canUpload", canUpload);
            } else {
                photos = photoService.getAllPhotos();
                // For the general gallery view, user can't upload (no specific event selected)
                model.addAttribute("canUpload", false);
            }

            model.addAttribute("photos", photos);
            model.addAttribute("events", events);
            
            // Add current user email for display
            if (sessionUser != null) {
                model.addAttribute("currentUserEmail", sessionUser.email());
            }
            
            return "gallery-view";
        } catch (Exception e) {
            logger.error("Error loading gallery: ", e);
            model.addAttribute("errorMessage", "Failed to load gallery");
            return "error";
        }
    }

    @PostMapping("/upload")
    public String uploadPhoto(
            @RequestParam("eventId") int eventId,
            @RequestParam("photo") MultipartFile file,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            if (session != null) {
                sessionUser = (User) session.getAttribute("user");
            }
            
            logger.info("Upload attempt for event {} by user: {}", eventId, 
                       sessionUser != null ? sessionUser.email() : "anonymous");
            
            // Check if user is authorized to upload to this event
            if (!canUploadToEvent(sessionUser, eventId)) {
                logger.warn("Unauthorized photo upload attempt by user {} for event {}", 
                           sessionUser != null ? sessionUser.email() : "anonymous", eventId);
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You are not authorized to upload photos to this event. Only the event organiser can upload photos.");
                return "redirect:/gallery/event/" + eventId;
            }

            // Get the organiser ID for this user
            Optional<Integer> organiserIdOpt = getOrganiserIdForUser(sessionUser);
            if (organiserIdOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unable to determine organiser permissions.");
                return "redirect:/gallery/event/" + eventId;
            }
            int organiserId = organiserIdOpt.get();

            Event_Photos uploadedPhoto = photoService.uploadPhoto(eventId, organiserId, file);
            
            logger.info("Photo uploaded successfully: {}", uploadedPhoto.url());
            redirectAttributes.addFlashAttribute("flashMessage", "Photo uploaded successfully!");
            
            return "redirect:/gallery/event/" + eventId;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid photo upload attempt: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/gallery/event/" + eventId;
        } catch (Exception e) {
            logger.error("Error uploading photo: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload photo");
            return "redirect:/gallery";
        }
    }

    @PostMapping("/upload-multiple")
    public String uploadMultiplePhotos(
            @RequestParam("eventId") int eventId,
            @RequestParam("photos") List<MultipartFile> files,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            if (session != null) {
                sessionUser = (User) session.getAttribute("user");
            }
            
            logger.info("Multiple photo upload attempt for event {} by user: {} ({} files)", eventId, 
                       sessionUser != null ? sessionUser.email() : "anonymous", files.size());
            
            // Check if user is authorized to upload to this event
            if (!canUploadToEvent(sessionUser, eventId)) {
                logger.warn("Unauthorized multiple photo upload attempt by user {} for event {}", 
                           sessionUser != null ? sessionUser.email() : "anonymous", eventId);
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You are not authorized to upload photos to this event. Only the event organiser can upload photos.");
                return "redirect:/gallery/event/" + eventId;
            }

            // Get the organiser ID for this user
            Optional<Integer> organiserIdOpt = getOrganiserIdForUser(sessionUser);
            if (organiserIdOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unable to determine organiser permissions.");
                return "redirect:/gallery/event/" + eventId;
            }
            int organiserId = organiserIdOpt.get();

            List<Event_Photos> uploadedPhotos = photoService.uploadMultiplePhotos(eventId, organiserId, files);
            
            logger.info("{} photos uploaded successfully for event {}", uploadedPhotos.size(), eventId);
            redirectAttributes.addFlashAttribute("flashMessage", 
                uploadedPhotos.size() + " photo(s) uploaded successfully!");
            
            return "redirect:/gallery/event/" + eventId;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid multiple photo upload attempt: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/gallery/event/" + eventId;
        } catch (Exception e) {
            logger.error("Error uploading multiple photos: ", e);
            // Check if the error contains information about partial success
            if (e.getMessage().contains("Successfully uploaded")) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload photos");
            }
            return "redirect:/gallery/event/" + eventId;
        }
    }

    @PostMapping("/delete/{photoId}")
    public String deletePhoto(
            @PathVariable int photoId,
            @RequestParam(value = "eventId", required = false) Integer eventId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            if (session != null) {
                sessionUser = (User) session.getAttribute("user");
            }
            
            // Check if user is logged in
            if (sessionUser == null) {
                logger.warn("Unauthorized photo delete attempt - user not logged in");
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to delete photos.");
                return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
            }
            
            Optional<Event_Photos> photo = photoService.getPhotoById(photoId);
            if (photo.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Photo not found");
                return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
            }
            
            // Get the event ID from the photo if not provided in request
            int targetEventId = eventId != null ? eventId : photo.get().event_id();
            
            // Check if user is authorized to delete photos from this event
            if (!canUploadToEvent(sessionUser, targetEventId)) {
                logger.warn("Unauthorized photo delete attempt by user {} for event {}", 
                           sessionUser.email(), targetEventId);
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You are not authorized to delete photos from this event. Only the event organiser can delete photos.");
                return "redirect:/gallery/event/" + targetEventId;
            }
            
            photoService.deletePhoto(photoId);
            
            logger.info("Photo deleted successfully: ID {} by user {}", photoId, sessionUser.email());
            redirectAttributes.addFlashAttribute("flashMessage", "Photo deleted successfully!");
            
            return "redirect:/gallery/event/" + targetEventId;
            
        } catch (Exception e) {
            logger.error("Error deleting photo: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete photo");
            return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
        }
    }

    @PostMapping("/delete-multiple")
    public String deleteMultiplePhotos(
            @RequestParam("photoIds") List<String> photoIdStrings,
            @RequestParam(value = "eventId", required = false) Integer eventId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        try {
            HttpSession session = request.getSession(false);
            User sessionUser = null;
            if (session != null) {
                sessionUser = (User) session.getAttribute("user");
            }
            
            // Check if user is logged in
            if (sessionUser == null) {
                logger.warn("Unauthorized bulk photo delete attempt - user not logged in");
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to delete photos.");
                return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
            }
            
            // Convert string photo IDs to integers
            List<Integer> photoIds = new ArrayList<>();
            for (String photoIdStr : photoIdStrings) {
                try {
                    photoIds.add(Integer.parseInt(photoIdStr));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid photo ID format: {}", photoIdStr);
                }
            }
            
            if (photoIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No valid photos selected for deletion.");
                return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
            }
            
            // Validate that all photos belong to events the user can manage
            // and get the event ID if not provided
            int targetEventId = eventId != null ? eventId : -1;
            
            for (Integer photoId : photoIds) {
                Optional<Event_Photos> photo = photoService.getPhotoById(photoId);
                if (photo.isEmpty()) {
                    logger.warn("Photo not found: {}", photoId);
                    redirectAttributes.addFlashAttribute("errorMessage", "One or more selected photos were not found.");
                    return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
                }
                
                // Set targetEventId from first photo if not provided
                if (targetEventId == -1) {
                    targetEventId = photo.get().event_id();
                }
                
                // Verify user can delete from this event
                if (!canUploadToEvent(sessionUser, photo.get().event_id())) {
                    logger.warn("Unauthorized bulk photo delete attempt by user {} for event {}", 
                               sessionUser.email(), photo.get().event_id());
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "You are not authorized to delete photos from this event. Only the event organiser can delete photos.");
                    return "redirect:/gallery/event/" + photo.get().event_id();
                }
            }
            
            photoService.deleteMultiplePhotos(photoIds);
            
            logger.info("Multiple photos deleted successfully: {} photos by user {}", photoIds.size(), sessionUser.email());
            redirectAttributes.addFlashAttribute("flashMessage", 
                photoIds.size() + " photo(s) deleted successfully!");
            
            return targetEventId != -1 ? "redirect:/gallery/event/" + targetEventId : "redirect:/gallery";
            
        } catch (Exception e) {
            logger.error("Error deleting multiple photos: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete selected photos");
            return eventId != null ? "redirect:/gallery/event/" + eventId : "redirect:/gallery";
        }
    }

    @GetMapping("/api/events/{eventId}/photos")
    public ResponseEntity<List<Event_Photos>> getEventPhotos(@PathVariable int eventId) {
        try {
            List<Event_Photos> photos = photoService.getEventPhotos(eventId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.error("Error fetching photos for event {}: ", eventId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to check if the current user can upload photos to a specific event.
     * Only the organiser of the event can upload photos.
     */
    private boolean canUploadToEvent(User user, int eventId) {
        if (user == null) {
            logger.debug("canUploadToEvent: No user (not logged in) for event {}", eventId);
            return false;
        }

        try {
            logger.debug("canUploadToEvent: Checking permissions for user {} and event {}", user.email(), eventId);
            
            logger.debug("canUploadToEvent: Found user with ID {} for email {}", user.user_id(), user.email());

            // Check if user has an organiser profile
            Optional<Integer> organiserIdOpt = userRepository.getOrganiserIdByUserId(user.user_id());
            if (organiserIdOpt.isEmpty()) {
                logger.debug("canUploadToEvent: User {} (ID: {}) is not an organiser", user.email(), user.user_id());
                return false;
            }
            logger.debug("canUploadToEvent: User {} (ID: {}) has organiser profile with ID {}", 
                        user.email(), user.user_id(), organiserIdOpt.get());

            // Use the repository method to check if this user is the organiser for this event
            boolean isOrganiser = userRepository.isOrganiserForEvent(user.user_id(), eventId);
            logger.debug("canUploadToEvent: User {} {} organiser for event {}", 
                        user.email(), isOrganiser ? "IS" : "is NOT", eventId);
            
            return isOrganiser;
            
        } catch (Exception e) {
            logger.error("Error checking upload permissions for user {} and event {}: ", 
                        user.email(), eventId, e);
            return false;
        }
    }

    /**
     * Helper method to get the organiser ID for the current user.
     */
    private Optional<Integer> getOrganiserIdForUser(User user) {
        if (user == null) {
            return Optional.empty();
        }

        try {
            return userRepository.getOrganiserIdByUserId(user.user_id());
            
        } catch (Exception e) {
            logger.error("Error getting organiser ID for user {}: ", user.email(), e);
            return Optional.empty();
        }
    }
}
