package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);
    
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DataSource dataSource;

    public NotificationScheduler(UserRepository userRepository,
                                NotificationService notificationService,
                                DataSource dataSource) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.dataSource = dataSource;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendEventReminders() {
        logger.info("Starting scheduled event reminder task");
        
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            
            List<Event> upcomingEvents = findEventsByDate(tomorrow);
            
            logger.info("Found {} events for tomorrow ({})", upcomingEvents.size(), tomorrow);
            
            for (Event event : upcomingEvents) {
                sendRemindersForEvent(event);
            }
            
            logger.info("Completed event reminder task");
        } catch (Exception e) {
            logger.error("Error in scheduled event reminder task", e);
        }
    }

    private List<Event> findEventsByDate(LocalDate date) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Events WHERE date = ? AND status != 'CANCELLED'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, date);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event event = new Event(
                        rs.getInt("event_id"),
                        rs.getObject("organiser_id", Long.class),
                        rs.getObject("club_id", Long.class),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getDate("date").toLocalDate(),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTime("finish_time").toString(),
                        rs.getString("status"),
                        rs.getInt("capacity")
                    );
                    events.add(event);
                }
            }
        } catch (Exception e) {
            logger.error("Error finding events by date", e);
        }
        
        return events;
    }

    private void sendRemindersForEvent(Event event) {
        try {
            List<Integer> userIds = getUserIdsForEvent(event.event_id());
            
            logger.info("Sending reminders for event '{}' to {} attendees", 
                       event.title(), userIds.size());
            
            for (Integer userId : userIds) {
                sendReminderToUser(userId, event);
            }
        } catch (Exception e) {
            logger.error("Error sending reminders for event {}", event.event_id(), e);
        }
    }

    private List<Integer> getUserIdsForEvent(int eventId) {
        List<Integer> userIds = new ArrayList<>();
        String sql = "SELECT user_id FROM RSVP WHERE event_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
        } catch (Exception e) {
            logger.error("Error getting user IDs for event {}", eventId, e);
        }
        
        return userIds;
    }

    private void sendReminderToUser(int userId, Event event) {
        try {
            userRepository.findById((long) userId).ifPresent(user -> {
                String userName = getUserName(userId);
                String eventDate = event.formattedStartDateTime();
                
                notificationService.notifyEventReminder(
                    userId,
                    user.email(),
                    userName,
                    event.event_id(),
                    event.title(),
                    eventDate,
                    event.location()
                );
            });
        } catch (Exception e) {
            logger.error("Error sending reminder to user {}", userId, e);
        }
    }


    private String getUserName(int userId) {
        try {
            String sql = "SELECT name FROM Student_Profile WHERE user_id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        if (name != null && !name.isBlank()) {
                            return name;
                        }
                    }
                }
            }
            
            return userRepository.findById((long) userId)
                .map(user -> user.email().split("@")[0])
                .orElse("Student");
        } catch (Exception e) {
            logger.error("Error getting user name for user {}", userId, e);
            return "Student";
        }
    }
}
