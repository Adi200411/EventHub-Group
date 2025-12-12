package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.NotificationDTO;
import au.edu.rmit.sept.webapp.model.Notification;
import au.edu.rmit.sept.webapp.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationServiceImpl(NotificationRepository notificationRepository, 
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Override
    public Notification createNotification(NotificationDTO dto) {
        Notification notification = new Notification(
            0, 
            dto.getUserId(),
            dto.getType(),
            dto.getTitle(),
            dto.getMessage(),
            dto.getLink(),
            LocalDateTime.now(),
            null 
        );
        
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getUserNotifications(int userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(int userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(int notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(int userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Override
    public void deleteNotification(int notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public int getUnreadCount(int userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    public void notifyRsvpConfirmation(int userId, String userEmail, String userName, 
                                       int eventId, String eventTitle, String eventDate, 
                                       String eventLocation) {
        try {
            NotificationDTO dto = new NotificationDTO(
                userId,
                "RSVP_CONFIRMATION",
                "RSVP Confirmed",
                String.format("Your RSVP for '%s' has been confirmed!", eventTitle),
                "/events/" + eventId
            );
            createNotification(dto);
            
            emailService.sendRsvpConfirmationEmail(userEmail, userName, eventTitle, 
                                                  eventDate, eventLocation);
            
            logger.info("RSVP confirmation notification sent to user {}", userId);
        } catch (Exception e) {
            logger.error("Error sending RSVP confirmation notification", e);
        }
    }

    @Override
    public void notifyEventReminder(int userId, String userEmail, String userName, 
                                    int eventId, String eventTitle, String eventDate, 
                                    String eventLocation) {
        try {
            NotificationDTO dto = new NotificationDTO(
                userId,
                "EVENT_REMINDER",
                "Event Reminder",
                String.format("Reminder: '%s' is tomorrow!", eventTitle),
                "/events/" + eventId
            );
            createNotification(dto);
            
            emailService.sendEventReminderEmail(userEmail, userName, eventTitle, 
                                               eventDate, eventLocation);
            
            logger.info("Event reminder notification sent to user {}", userId);
        } catch (Exception e) {
            logger.error("Error sending event reminder notification", e);
        }
    }

    @Override
    public void notifyNewEvent(int userId, int eventId, String eventTitle, String eventDate) {
        try {
            //create notification in database
            NotificationDTO dto = new NotificationDTO(
                userId,
                "NEW_EVENT",
                "New Event Available",
                String.format("Check out the new event: '%s'", eventTitle),
                "/events/" + eventId
            );
            createNotification(dto);
            
            logger.info("New event notification sent to user {}", userId);
        } catch (Exception e) {
            logger.error("Error sending new event notification", e);
        }
    }
}
