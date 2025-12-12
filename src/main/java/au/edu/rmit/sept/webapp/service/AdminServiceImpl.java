package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final EventRepository eventRepository;

    public AdminServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public List<Event> getAllEvents() {
        logger.info("Fetching all events for admin view");
        return eventRepository.listAllEvents();   // ✅ fixed
    }

    @Override
    public Optional<Event> getEventById(Long id) {
        logger.info("Fetching event with ID: {}", id);
        return eventRepository.findById(id);      // ✅ already exists
    }

    @Override
    public void updateEvent(Event updatedEvent) {
        logger.info("Updating event with ID: {}", updatedEvent.event_id());
        eventRepository.updateEvent(updatedEvent);  // ✅ pass new Event record
    }

    @Override
    public int deleteEventById(Long eventId, Long adminId, String reason) {
        return eventRepository.deleteEventById(eventId.intValue(), adminId, reason);
    }

    @Override
    public List<Event> getDeletedEvents() {
        return eventRepository.findDeletedEvents();
    }

    @Override
    public List<DeletedEventLog> getDeletedEventLogs() {
        return eventRepository.findDeletedLogs();
    }


}
