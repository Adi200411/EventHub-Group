package au.edu.rmit.sept.webapp.service;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import au.edu.rmit.sept.webapp.dto.RsvpUserDetail;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.repository.EventRepositoryImpl;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    private final EventRepositoryImpl eventRepository;

    public EventServiceImpl(EventRepositoryImpl repository) {
        this.eventRepository = repository;
    }

    @Override
    public List<Event> getAllEvents() {
        return eventRepository.listAllEvents();
    }

    @Override
    public List<Event> getUpcomingEvents() {
        try {
            logger.info("Fetching upcoming events from database");
            List<Event> upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now());
            logger.info("Successfully retrieved {} upcoming events", upcomingEvents.size());
            return upcomingEvents;
        } catch (Exception e) {
            logger.error("Error fetching upcoming events: ", e);
            return eventRepository.listAllEvents(); // Fallback to all events
        }
    }

    @Override
    public List<Event> getPastEvents() {
        try {
            logger.info("Fetching past events from database");
            List<Event> pastEvents = eventRepository.findPastEvents(LocalDateTime.now());
            logger.info("Successfully retrieved {} past events", pastEvents.size());
            return pastEvents;
        } catch (Exception e) {
            logger.error("Error fetching past events: ", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Event> searchEvents(String query, String tag, String date, boolean isUpcoming) {
        try {
            logger.info("Searching events with params");
            LocalDate localDate = StringUtils.hasText(date) ? LocalDate.parse(date) : null;
            return eventRepository.searchEvents(query, tag, localDate, isUpcoming, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error searching events: ", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Event> getRecommendedEvents(Long userId) {
        try {
            var now = LocalDateTime.now();
            int pageable = 8; 
            var recs = eventRepository.findRecommendedBySimilarity(userId, now, pageable);
            
            if (recs == null || recs.isEmpty()) {
                return Collections.emptyList(); 
            }
            return recs;
        } catch (Exception e) {
            logger.error("Error fetching recommended events for user {}: ", userId, e);
            // FIXED: Returns an empty list on error
            return Collections.emptyList();
        }
    }

    @Override
    public List<Event> getRsvpedEvents(Long userId) {
        try {
            logger.info("Fetching RSVP'd events for user {}", userId);
            return eventRepository.findRsvpedEventsByUserId(userId);
        } catch (Exception e) {
            logger.error("Error fetching RSVP'd events for user {}: ", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Integer> getRsvpedEventIds(Long userId) {
        return eventRepository.findRsvpedEventIds(userId)
                .stream()
                .map(id -> id.intValue())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void rsvp(Long eventId, Long userId) {
        eventRepository.rsvp(eventId, userId);
    }

    @Override
    public void cancelRsvp(Long eventId, Long userId) {
        eventRepository.cancelRsvp(eventId, userId);
    }

    @Override
    public java.util.List<RsvpUserDetail> getRsvpedUsersForEvent(java.lang.Long eventId) {
        try {
            return eventRepository.findRsvpDetailsByEventId(eventId);
        } catch (Exception e) {
            logger.error("Error fetching RSVP details for event {}: ", eventId, e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public List<Event> getPastEventsAttendedByUser(Long userId) {
        try {
            logger.info("Fetching past events attended by user {}", userId);
            List<Event> pastEventsAttended = eventRepository.findPastEventsAttendedByUser(userId, java.time.LocalDateTime.now());
            logger.info("Successfully retrieved {} past events attended by user {}", pastEventsAttended.size(), userId);
            return pastEventsAttended;
        } catch (Exception e) {
            logger.error("Error fetching past events attended by user {}: ", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean checkInUser(Long eventId, Long userId) {
        return eventRepository.checkInUser(eventId, userId);
    }
    
    // ==== ADD-ONLY: richer upcoming search implementation (no Pageable) ====

@Override
public java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFiltered(
        String query, java.lang.Integer page, java.lang.Integer size) {

    final String q = normalizeBlankLocal(query);
    final java.time.LocalDate today = java.time.LocalDate.now();
    final java.time.LocalDateTime now = java.time.LocalDateTime.now();

    java.lang.Integer limit = null;
    java.lang.Integer offset = null;
    if (page != null && size != null && page >= 0 && size > 0) {
        limit = size;
        offset = page * size;
    }

    // Delegates to the add-only repo API (must be present in EventRepository + Impl)
    return eventRepository.searchUpcomingEnhanced(q, today, now, limit, offset);
}

@Override
public java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFiltered(String query) {
    return getUpcomingFiltered(query, null, null);
}

@Override
public java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFilteredByOrganiser(Long organiserId, String query, java.lang.Integer page, java.lang.Integer size) {
    if (organiserId == null) {
        return java.util.List.of();
    }
    return eventRepository.searchUpcomingByOrganiser(organiserId, normalizeBlankLocal(query), page, size);
}

// ---- local helper (kept private to avoid name clashes with existing helpers) ----
private static String normalizeBlankLocal(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
}


// ==== ADD-ONLY: real CRUD implementations ====

@java.lang.Override
public java.util.Optional<au.edu.rmit.sept.webapp.model.Event> getEventById(java.lang.Long id) {
    return eventRepository.findById(id);
}

@java.lang.Override
public au.edu.rmit.sept.webapp.model.Event createEvent(au.edu.rmit.sept.webapp.model.Event event) {
    int newId = eventRepository.insertEvent(event);
    return eventRepository.findById((long) newId)
            .orElseThrow(() -> new IllegalStateException("Inserted event not found id=" + newId));
}

@java.lang.Override
public au.edu.rmit.sept.webapp.model.Event updateEvent(java.lang.Long id, au.edu.rmit.sept.webapp.model.Event event) {
    // Ensure the event_id in the record matches path id
    au.edu.rmit.sept.webapp.model.Event toSave = new au.edu.rmit.sept.webapp.model.Event(
            id.intValue(),
            event.organiser_id(),
            event.club_id(),
            event.title(),
            event.description(),
            event.location(),
            event.date(),
            event.start_time(),
            event.finish_time(),
            event.status(),
            event.capacity()
    );
    int rows = eventRepository.updateEvent(toSave);
    if (rows == 0) throw new IllegalStateException("Update failed or no rows affected for id=" + id);
    return eventRepository.findById(id).orElse(toSave);
}

@java.lang.Override
public void deleteEvent(java.lang.Long id) {
    int rows = eventRepository.cancelEventById(id);
    if (rows == 0) throw new IllegalStateException("Cancel failed or no rows affected for id=" + id);
}


@Override
public int deleteEventById(int eventId, Long adminId, String reason) {
    return eventRepository.deleteEventById(eventId, adminId, reason);
}

@Override
public List<Event> getDeletedEvents() {
    return eventRepository.findDeletedEvents();
}

    @Override
    public void updateEventTags(int eventId, String tags) {
        eventRepository.clearTagsForEvent(eventId);
        if (tags != null && !tags.isBlank()) {
            String[] tagNames = tags.split(",");
            for (String tagName : tagNames) {
                String trimmedTagName = tagName.trim();
                if (!trimmedTagName.isEmpty()) {
                    Optional<Integer> tagIdOpt = eventRepository.findTagByName(trimmedTagName);
                    int tagId = tagIdOpt.orElseGet(() -> eventRepository.createTag(trimmedTagName));
                    eventRepository.linkTagToEvent(eventId, tagId);
                }
            }
        }
    }
    
    @Override
    public List<au.edu.rmit.sept.webapp.model.Tags> getAllTags() {
        return eventRepository.findAllTags();
    }

    @Override
    public List<au.edu.rmit.sept.webapp.model.Tags> getTagsByEventId(int eventId) {
        return eventRepository.findTagsByEventId(eventId);
    }
}