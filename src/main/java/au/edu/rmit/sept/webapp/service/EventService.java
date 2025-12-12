package au.edu.rmit.sept.webapp.service;

import java.util.List;

import au.edu.rmit.sept.webapp.dto.RsvpUserDetail;
import au.edu.rmit.sept.webapp.model.Event;

public interface  EventService {
    public List<Event> getAllEvents(); 

    public List<Event> getUpcomingEvents();

    public List<Event> getPastEvents();

    public List<Event> searchEvents(String query, String tag, String date, boolean isUpcoming);

    public List<Event> getRecommendedEvents(Long userId);
    
    public List<Integer> getRsvpedEventIds(Long userId);

    public void rsvp(Long eventId, Long userId);

    public void cancelRsvp(Long eventId, Long userId);

    public List<RsvpUserDetail> getRsvpedUsersForEvent(Long eventId);

    List<Event> getRsvpedEvents(Long userId);

    List<Event> getPastEventsAttendedByUser(Long userId);

    boolean checkInUser(Long eventId, Long userId);

    // ==== ADD-ONLY: richer upcoming search (no Pageable) ====

/**
 * Enhanced upcoming search (no Pageable).
 * - Gates by (date > today) OR (date = today AND start_time >= now)
 * - Excludes CANCELLED
 * - Case-insensitive match over title/description/location
 * - Returns items ordered by (date ASC, start_time ASC, title ASC)
 * - If page & size are provided, service applies LIMIT/OFFSET at repo layer
 */
java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFiltered(String query, java.lang.Integer page, java.lang.Integer size);

/** Convenience overload without pagination. */
java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFiltered(String query);

/** Get upcoming events filtered by organiser ID and optional search query. */
java.util.List<au.edu.rmit.sept.webapp.model.Event> getUpcomingFilteredByOrganiser(Long organiserId, String query, java.lang.Integer page, java.lang.Integer size);

// ==== ADD-ONLY: CRUD used by organiser pages ====
java.util.Optional<au.edu.rmit.sept.webapp.model.Event> getEventById(java.lang.Long id);
au.edu.rmit.sept.webapp.model.Event createEvent(au.edu.rmit.sept.webapp.model.Event event);
au.edu.rmit.sept.webapp.model.Event updateEvent(java.lang.Long id, au.edu.rmit.sept.webapp.model.Event event);
void deleteEvent(java.lang.Long id);
public int deleteEventById(int eventId, Long adminId, String reason);
public List<Event> getDeletedEvents();

void updateEventTags(int eventId, String tags);
List<au.edu.rmit.sept.webapp.model.Tags> getAllTags();
List<au.edu.rmit.sept.webapp.model.Tags> getTagsByEventId(int eventId);
}
