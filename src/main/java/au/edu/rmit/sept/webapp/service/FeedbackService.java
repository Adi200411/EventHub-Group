package au.edu.rmit.sept.webapp.service;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Event_Feedback;

public interface FeedbackService {
    
    Event_Feedback submitFeedback(int eventId, int userId, int rating, String comments);
    
    List<Event_Feedback> getEventFeedback(int eventId);
    
    Optional<Event_Feedback> getUserEventFeedback(int eventId, int userId);
    
    boolean hasUserProvidedFeedback(int eventId, int userId);
    
    double getAverageRating(int eventId);
    
    List<Event_Feedback> getUserFeedback(int userId);
}