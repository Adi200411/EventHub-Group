package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    
    Notification save(Notification notification);
    
    Optional<Notification> findById(int notificationId);
    
    List<Notification> findByUserId(int userId);
    
    List<Notification> findUnreadByUserId(int userId);
    
    void markAsRead(int notificationId);

    void markAllAsReadForUser(int userId);
    
    void deleteById(int notificationId);

    void deleteAllForUser(int userId);

    int countUnreadByUserId(int userId);
}
