package au.edu.rmit.sept.webapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${app.email.from:noreply@eventhub.rmit.edu.au}")
    private String fromEmail;

    @Override
    public void sendRsvpConfirmationEmail(String toEmail, String userName, String eventTitle, 
                                          String eventDate, String eventLocation) {
        String subject = "RSVP Confirmation - " + eventTitle;
        String message = buildRsvpConfirmationMessage(userName, eventTitle, eventDate, eventLocation);
        
        sendEmail(toEmail, subject, message);
        logger.info("RSVP confirmation email sent to: {}", toEmail);
    }

    @Override
    public void sendEventReminderEmail(String toEmail, String userName, String eventTitle, 
                                       String eventDate, String eventLocation) {
        String subject = "Reminder: " + eventTitle + " is Tomorrow!";
        String message = buildEventReminderMessage(userName, eventTitle, eventDate, eventLocation);
        
        sendEmail(toEmail, subject, message);
        logger.info("Event reminder email sent to: {}", toEmail);
    }

    @Override
    public void sendNewEventEmail(String toEmail, String userName, String eventTitle, String eventDate) {
        String subject = "New Event: " + eventTitle;
        String message = buildNewEventMessage(userName, eventTitle, eventDate);
        
        sendEmail(toEmail, subject, message);
        logger.info("New event notification email sent to: {}", toEmail);
    }

    @Override
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        sendEmail(toEmail, subject, message);
        logger.info("Notification email sent to: {}", toEmail);
    }

    private void sendEmail(String toEmail, String subject, String message) {
        //loging the email content for debugging/demo purposes
        logger.info("\n" + "=".repeat(80));
        logger.info("EMAIL NOTIFICATION");
        logger.info("=".repeat(80));
        logger.info("From: {}", fromEmail);
        logger.info("To: {}", toEmail);
        logger.info("Subject: {}", subject);
        logger.info("-".repeat(80));
        logger.info("Message:\n{}", message);
        logger.info("=".repeat(80) + "\n");
        
        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(fromEmail);
                mailMessage.setTo(toEmail);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                
                mailSender.send(mailMessage);
                logger.info("✓ Email successfully sent via SMTP to: {}", toEmail);
            } catch (Exception e) {
                logger.error("✗ Failed to send email to {}: {}", toEmail, e.getMessage());
                logger.warn("Email logged above but not delivered. Check SMTP configuration.");
            }
        } else if (emailEnabled && mailSender == null) {
            logger.warn("Email enabled but JavaMailSender not configured. Check spring-boot-starter-mail dependency.");
        } else {
            logger.info("Email sending disabled (app.email.enabled=false). Email logged only.");
        }
    }

    private String buildRsvpConfirmationMessage(String userName, String eventTitle, 
                                                String eventDate, String eventLocation) {
        return String.format("""
            Hi %s,
            
            Thank you for your RSVP! We're excited to confirm your attendance at:
            
            Event: %s
            Date: %s
            Location: %s
            
            We look forward to seeing you there!
            
            If you need to cancel your RSVP, please log in to EventHub and manage your bookings.
            
            Best regards,
            The EventHub Team
            RMIT University
            """, userName, eventTitle, eventDate, eventLocation);
    }

    private String buildEventReminderMessage(String userName, String eventTitle, 
                                             String eventDate, String eventLocation) {
        return String.format("""
            Hi %s,
            
            This is a friendly reminder that you have an upcoming event tomorrow:
            
            Event: %s
            Date: %s
            Location: %s
            
            Don't forget to bring your student ID for check-in!
            
            See you there!
            
            Best regards,
            The EventHub Team
            RMIT University
            """, userName, eventTitle, eventDate, eventLocation);
    }

    private String buildNewEventMessage(String userName, String eventTitle, String eventDate) {
        return String.format("""
            Hi %s,
            
            A new event that might interest you has been added to EventHub:
            
            Event: %s
            Date: %s
            
            Log in to EventHub to view event details and RSVP!
            
            Best regards,
            The EventHub Team
            RMIT University
            """, userName, eventTitle, eventDate);
    }
}
