package au.edu.rmit.sept.webapp.repository;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Event_Photos;

public interface PhotoRepository {
    
    Event_Photos save(Event_Photos photo);
    
    Optional<Event_Photos> findById(int photoId);
    
    List<Event_Photos> findByEventId(int eventId);
    
    List<Event_Photos> findByOrganiserId(int organiserId);
    
    void deleteById(int photoId);
    
    void deleteMultipleById(List<Integer> photoIds);
    
    List<Event_Photos> findAll();
}