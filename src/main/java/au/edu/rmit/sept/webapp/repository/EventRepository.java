package au.edu.rmit.sept.webapp.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.dto.RsvpUserDetail;
import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Tags;

public interface EventRepository {

    List<Event> listAllEvents();

    List<Event> findRecommendedBySimilarity(Long userId, LocalDateTime now, int limit);

    List<Integer> findRsvpedEventIds(Long userId);

    int rsvp(Long eventId, Long userId);

    int cancelRsvp(Long eventId, Long userId);

    boolean checkInUser(Long eventId, Long userId);

    List<Event> findUpcomingEvents(LocalDateTime currentDate);

    List<RsvpUserDetail> findRsvpDetailsByEventId(Long eventId);
    List<Event> findPastEvents(LocalDateTime currentDate);

    List<Event> searchEvents(String query, String tag, LocalDate date, boolean isUpcoming, LocalDateTime now);

    List<Event> findRsvpedEventsByUserId(Long userId);

    List<Event> findPastEventsAttendedByUser(Long userId, LocalDateTime currentDate);

    // === ADD-ONLY: Enhanced upcoming search (default method to keep existing impls safe) ===

/**
 * Enhanced upcoming search that mirrors legacy "Pageable" behaviour without exposing Pageable.
 * - Upcoming gate: (date > today) OR (date = today AND start_time >= now)
 * - Excludes CANCELLED (case-insensitive)
 * - Case-insensitive match over title/description/location when q is non-blank
 * - Default ordering: date ASC, start_time ASC, title ASC
 * - Optional LIMIT/OFFSET via nullable limit/offset (apply in repo impl only if both provided)
 *
 * NOTE: Default implementation delegates to existing searchEvents(...)
 * to remain backward-compatible. Repo implementations may @Override
 * for true SQL-level LIMIT/OFFSET and stricter time gating.
 */
default java.util.List<au.edu.rmit.sept.webapp.model.Event> searchUpcomingEnhanced(
        String q,
        java.time.LocalDate today,
        java.time.LocalDateTime now,
        java.lang.Integer limit,
        java.lang.Integer offset
) {
    // Fallback: adapt to existing searchEvents() contract if present
    java.time.LocalDate useDate = (today != null ? today : java.time.LocalDate.now());
    java.time.LocalDateTime useNow = (now != null ? now : java.time.LocalDateTime.now());
    // If your interface has searchEvents(String query, String tag, LocalDate date, boolean isUpcoming, LocalDateTime now)
    try {
        return this.searchEvents(q, null, useDate, true, useNow);
    } catch (Throwable ignore) {
        // If not available, return empty by default; impl should override.
        return java.util.List.of();
    }
}

java.util.Optional<au.edu.rmit.sept.webapp.model.Event> findById(java.lang.Long id);

/** @return generated event_id (INT) */
int insertEvent(au.edu.rmit.sept.webapp.model.Event event);

/** @return rows affected (0/1) */
int updateEvent(au.edu.rmit.sept.webapp.model.Event event);

/** Soft delete: sets status='CANCELLED'. @return rows affected (0/1) */
int cancelEventById(java.lang.Long id);

int deleteEventById(int eventId, Long adminId, String reason);
List<Event> findDeletedEvents();

List<DeletedEventLog> findDeletedLogs();

void clearTagsForEvent(int eventId);

Optional<Integer> findTagByName(String tagName);

int createTag(String tagName);

void linkTagToEvent(int eventId, int tagId);

List<Tags> findAllTags();

List<Tags> findTagsByEventId(int eventId);

List<Event> searchUpcomingByOrganiser(Long organiserId, String query, Integer page, Integer size);

}