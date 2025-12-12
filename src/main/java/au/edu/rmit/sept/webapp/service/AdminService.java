package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import java.util.List;
import java.util.Optional;

public interface AdminService {
    List<Event> getAllEvents();
    Optional<Event> getEventById(Long id);
    void updateEvent(Event event);  // ✅ updated
    int deleteEventById(Long eventId, Long adminId, String reason);
    List<Event> getDeletedEvents();
    List<DeletedEventLog> getDeletedEventLogs();
}
