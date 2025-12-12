package au.edu.rmit.sept.webapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import au.edu.rmit.sept.webapp.model.Event_Photos;

public interface PhotoService {
    
    Event_Photos uploadPhoto(int eventId, int organiserId, MultipartFile file);
    
    List<Event_Photos> uploadMultiplePhotos(int eventId, int organiserId, List<MultipartFile> files);
    
    List<Event_Photos> getEventPhotos(int eventId);
    
    List<Event_Photos> getAllPhotos();
    
    List<Event_Photos> getPhotosByOrganiser(int organiserId);
    
    Optional<Event_Photos> getPhotoById(int photoId);
    
    void deletePhoto(int photoId);
    
    void deleteMultiplePhotos(List<Integer> photoIds);
    
    boolean isValidImageFile(MultipartFile file);
    
    String saveFile(MultipartFile file, String fileName);
    
    List<Event_Photos> getEventPhotoThumbnails(int eventId, int limit);
}