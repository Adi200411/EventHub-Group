package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.NotificationDTO;
import au.edu.rmit.sept.webapp.model.Notification;
import java.util.List;

public interface NotificationService {
    
    Notification createNotification(NotificationDTO dto);
    
    List<Notification> getUserNotifications(int userId);
    
    List<Notification> getUnreadNotifications(int userId);
    
    void markAsRead(int notificationId);
    
    void markAllAsRead(int userId);
    
    void deleteNotification(int notificationId);
    
    int getUnreadCount(int userId);
    
    void notifyRsvpConfirmation(int userId, String userEmail, String userName, 
                                int eventId, String eventTitle, String eventDate, String eventLocation);
    
    void notifyEventReminder(int userId, String userEmail, String userName, 
                            int eventId, String eventTitle, String eventDate, String eventLocation);
    
    void notifyNewEvent(int userId, int eventId, String eventTitle, String eventDate);
}
