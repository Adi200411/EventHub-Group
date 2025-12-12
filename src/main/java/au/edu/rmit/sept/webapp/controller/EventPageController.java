package au.edu.rmit.sept.webapp.controller;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.dto.EventForm;
import au.edu.rmit.sept.webapp.dto.EventFormMapper;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/organiser/events")
public class EventPageController {

    private static final Logger log = LoggerFactory.getLogger(EventPageController.class);
    private final EventService eventService;
    private final UserService userService;

    public EventPageController(EventService eventService, UserService userService) {
        this.eventService = eventService;this.userService = userService;
    }

    // Access control with the emojis from helper
    private boolean isOrganizerOrAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return role != null && (role.equals("ORGANISER") || role.equals("ADMIN"));
    }

    //  LIST EVENTS (Organizer and  Admin)
    @GetMapping
    public String list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model)
        {
            HttpSession session = request.getSession(false); // false = don't create
            if (!isOrganizerOrAdmin(session)) {
                return "redirect:/access-denied";
            }

         try {
                 log.info("GET /organiser/events q={}, page={}, size={}", q, page, size);


                 int p = (page == null || page < 0) ? 0 : page;
                 int s = (size == null || size <= 0) ? 10 : size;
                 
                 List<Event> rows;
                 String role = (String) session.getAttribute("role");
                 
                 if ("ADMIN".equals(role)) {
                     // Admin can see all events
                     rows = eventService.getUpcomingFiltered(q, p, s);
                 } else {
                     // Organiser should only see their own events
                     Object organiserIdObj = session.getAttribute("organiserId");
                     Long organiserId = null;
                     if (organiserIdObj instanceof Integer) {
                         organiserId = ((Integer) organiserIdObj).longValue();
                     } else if (organiserIdObj instanceof Long) {
                         organiserId = (Long) organiserIdObj;
                     }
                     
                     if (organiserId != null) {
                         rows = eventService.getUpcomingFilteredByOrganiser(organiserId, q, p, s);
                     } else {
                         rows = new java.util.ArrayList<>();
                     }
                 }

                 Pageable pageable = PageRequest.of(p, s, Sort.unsorted());
                 Page<Event> pageObj = new PageImpl<>(rows, pageable, rows.size());

                 model.addAttribute("events", pageObj);
                 model.addAttribute("eventsList", rows);
                 model.addAttribute("q", (q == null) ? "" : q.trim());
                 model.addAttribute("page", p);
                 model.addAttribute("size", s);
                if (session != null && session.getAttribute("user") != null) {
                    model.addAttribute("user", session.getAttribute("user"));
                    model.addAttribute("authenticated", true);
                    Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
                    if (profileOpt.isPresent()) {
                        model.addAttribute("user_name", profileOpt.get().name());
                    }
                }

                 return "organiser/events_list";

             } catch (Exception e) {
                 log.error("Error loading organiser events", e);
                 return "error";
             }

    }

@PostMapping
public String create(@Valid @ModelAttribute("form") EventForm form,
                     BindingResult br,
                     HttpSession session,
                     RedirectAttributes ra) {

    if (!isOrganizerOrAdmin(session)) {
        return "redirect:/access-denied";
    }

    validateTimeOrder(form, br);
    if (br.hasErrors()) {
        ra.addFlashAttribute("org.springframework.validation.BindingResult.form", br);
        ra.addFlashAttribute("form", form);
        return "redirect:/organiser/events/new";
    }

    Long organiserId = null;
    Long clubId = null;

    String role = (String) session.getAttribute("role");
    Integer userId = (Integer) session.getAttribute("userId");

    if ("ADMIN".equals(role)) {
        // Admin event context
        organiserId = 1L; // or any admin-organiser placeholder
        clubId = null;    // adjust if needed
    } else {
        // Organiser context
        Optional<Organiser_Profile> organiserOpt = userService.findOrganiserById(userId);
        if (organiserOpt.isPresent()) {
            Organiser_Profile organiser = organiserOpt.get();
            organiserId = (long) organiser.organiser_id();
            clubId = (long) organiser.club_id();
        }
    }

    // ✅ Create event once
    if (organiserId != null) {
        Event entity = EventFormMapper.toNewEntity(form, organiserId, clubId);
        Event createdEvent = eventService.createEvent(entity);
        eventService.updateEventTags(createdEvent.event_id(), form.getTags());
        ra.addFlashAttribute("flash", "Event created successfully!");
    } else {
        ra.addFlashAttribute("flash", "Failed to identify organiser/admin for event creation.");
    }

    return "redirect:/organiser/events?created=1";
}


    //  EDIT FORM (Organiser and  Admin)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isOrganizerOrAdmin(session)) {
            return "redirect:/access-denied";
        }
        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!model.containsAttribute("form")) {
            EventForm form = EventFormMapper.fromEntity(event);
            List<au.edu.rmit.sept.webapp.model.Tags> tags = eventService.getTagsByEventId(id.intValue());
            String tagNames = tags.stream().map(au.edu.rmit.sept.webapp.model.Tags::tag_name).collect(java.util.stream.Collectors.joining(", "));
            form.setTags(tagNames);
            model.addAttribute("form", form);
        }
        if (session != null && session.getAttribute("user") != null) {
            model.addAttribute("user", session.getAttribute("user"));
            model.addAttribute("authenticated", true);
            Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
            if (profileOpt.isPresent()) {
                model.addAttribute("user_name", profileOpt.get().name());
            }
        }
        model.addAttribute("formMode", "edit");
        model.addAttribute("eventId", id);
        return "organiser/event_form";
    }

    // UPDATE EVENT (Organiser and Admin)
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") EventForm form,
                         BindingResult br,
                         HttpSession session,
                         RedirectAttributes ra) {

        if (!isOrganizerOrAdmin(session)) {
            return "redirect:/access-denied";
        }

        validateTimeOrder(form, br);
        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.form", br);
            ra.addFlashAttribute("form", form);
            return "redirect:/organiser/events/" + id + "/edit";
        }

        Event existing = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Event merged = EventFormMapper.applyToEntity(form, existing);
        eventService.updateEvent(id, merged);
        eventService.updateEventTags(id.intValue(), form.getTags());

        ra.addFlashAttribute("flash", "Event updated successfully.");
        return "redirect:/organiser/events?updated=1";
    }

    // CANCEL (Organiser and Admin)
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isOrganizerOrAdmin(session)) {
            return "redirect:/access-denied";
        }

        eventService.deleteEvent(id);
        ra.addFlashAttribute("flash", "Event cancelled.");
        return "redirect:/organiser/events?cancelled=1";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        if (!isOrganizerOrAdmin(session)) {
            return "redirect:/access-denied";
        }

        if (session != null && session.getAttribute("user") != null) {
            model.addAttribute("user", session.getAttribute("user"));
            model.addAttribute("authenticated", true);
            Optional<Student_Profile> profileOpt = userService.getProfileByUserId((int) session.getAttribute("userId"));
            if (profileOpt.isPresent()) {
                model.addAttribute("user_name", profileOpt.get().name());
            }
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new EventForm());
        }

        model.addAttribute("formMode", "create");
        return "organiser/event_form";
    }



private void validateTimeOrder(EventForm f, BindingResult result) {
    LocalTime s = f.getStartTime();
    LocalTime e = f.getEndTime();
    if (s == null  || e == null ) return;

    try {


        if (!e.isAfter(s)) {
            // End field message
            result.rejectValue("endTime", "EndBeforeStart", "End time must be after start time");
            // Start field message
            result.rejectValue("startTime", "StartNotBeforeEnd", "Start time must be before end time");
            }
        } catch (DateTimeParseException ignored) {
        }
    }
}