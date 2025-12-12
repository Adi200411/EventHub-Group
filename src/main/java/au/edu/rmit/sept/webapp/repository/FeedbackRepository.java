package au.edu.rmit.sept.webapp.repository;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Event_Feedback;

public interface FeedbackRepository {
    
    Event_Feedback save(Event_Feedback feedback);
    
    Optional<Event_Feedback> findById(int feedbackId);
    
    List<Event_Feedback> findByEventId(int eventId);
    
    List<Event_Feedback> findByUserId(int userId);
    
    Optional<Event_Feedback> findByEventIdAndUserId(int eventId, int userId);
    
    void deleteById(int feedbackId);
    
    double getAverageRatingByEventId(int eventId);
}