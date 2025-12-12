package au.edu.rmit.sept.webapp.service;

public interface EmailService {
    
    void sendRsvpConfirmationEmail(String toEmail, String userName, String eventTitle, String eventDate, String eventLocation);
    
    void sendEventReminderEmail(String toEmail, String userName, String eventTitle, String eventDate, String eventLocation);
    
    void sendNewEventEmail(String toEmail, String userName, String eventTitle, String eventDate);
    
    void sendNotificationEmail(String toEmail, String subject, String message);
}
