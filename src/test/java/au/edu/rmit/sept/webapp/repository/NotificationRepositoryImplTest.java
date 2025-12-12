package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Notification;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NotificationRepositoryImplTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    private NotificationRepository notificationRepository;

    @BeforeEach
    public void setUp() {
        flyway.clean();
        flyway.migrate();
        
        notificationRepository = new NotificationRepositoryImpl(dataSource);
    }

    @AfterEach
    public void tearDown() {
        flyway.clean();
    }

    @Test
    void testSaveNotification() {
        Notification notification = new Notification(
            0,
            1,
            "TEST",
            "Test Notification",
            "This is a test message",
            "/events/1",
            LocalDateTime.now(),
            null
        );

        Notification saved = notificationRepository.save(notification);

        assertNotNull(saved);
        assertTrue(saved.notification_id() > 0);
        assertEquals(1, saved.user_id());
        assertEquals("TEST", saved.type());
        assertEquals("Test Notification", saved.title());
        assertNull(saved.read_at());
    }

    @Test
    void testFindById() {
        Notification notification = createTestNotification(1, "RSVP_CONFIRMATION");
        Notification saved = notificationRepository.save(notification);
        Optional<Notification> found = notificationRepository.findById(saved.notification_id());
        assertTrue(found.isPresent());
        assertEquals(saved.notification_id(), found.get().notification_id());
    }

    @Test
    void testFindByUserId() {
        notificationRepository.save(createTestNotification(1, "RSVP_CONFIRMATION"));
        notificationRepository.save(createTestNotification(1, "EVENT_REMINDER"));
        notificationRepository.save(createTestNotification(2, "NEW_EVENT"));
        List<Notification> userNotifications = notificationRepository.findByUserId(1);
        assertEquals(2, userNotifications.size());
    }

    @Test
    void testFindUnreadByUserId() {
        Notification n1 = notificationRepository.save(createTestNotification(1, "RSVP_CONFIRMATION"));
        notificationRepository.save(createTestNotification(1, "EVENT_REMINDER"));
        notificationRepository.markAsRead(n1.notification_id());
        List<Notification> unread = notificationRepository.findUnreadByUserId(1);
        assertEquals(1, unread.size());
    }

    @Test
    void testMarkAsRead() {
        Notification notification = notificationRepository.save(createTestNotification(1, "TEST"));
        notificationRepository.markAsRead(notification.notification_id());
        Optional<Notification> updated = notificationRepository.findById(notification.notification_id());
        assertTrue(updated.isPresent());
        assertNotNull(updated.get().read_at());
    }

    @Test
    void testMarkAllAsReadForUser() {
        notificationRepository.save(createTestNotification(1, "RSVP_CONFIRMATION"));
        notificationRepository.save(createTestNotification(1, "EVENT_REMINDER"));
        notificationRepository.markAllAsReadForUser(1);
        List<Notification> unread = notificationRepository.findUnreadByUserId(1);
        assertEquals(0, unread.size());
    }

    @Test
    void testCountUnreadByUserId() {
        notificationRepository.save(createTestNotification(1, "RSVP_CONFIRMATION"));
        notificationRepository.save(createTestNotification(1, "EVENT_REMINDER"));
        notificationRepository.save(createTestNotification(1, "NEW_EVENT"));
        int count = notificationRepository.countUnreadByUserId(1);
        assertEquals(3, count);
    }

    @Test
    void testDeleteById() {
        Notification notification = notificationRepository.save(createTestNotification(1, "TEST"));
        notificationRepository.deleteById(notification.notification_id());
        Optional<Notification> deleted = notificationRepository.findById(notification.notification_id());
        assertFalse(deleted.isPresent());
    }

    @Test
    void testDeleteAllForUser() {
        notificationRepository.save(createTestNotification(1, "RSVP_CONFIRMATION"));
        notificationRepository.save(createTestNotification(1, "EVENT_REMINDER"));
        notificationRepository.save(createTestNotification(2, "NEW_EVENT"));
        notificationRepository.deleteAllForUser(1);
        List<Notification> user1Notifications = notificationRepository.findByUserId(1);
        List<Notification> user2Notifications = notificationRepository.findByUserId(2);
        
        assertEquals(0, user1Notifications.size());
        assertEquals(1, user2Notifications.size());
    }

    private Notification createTestNotification(int userId, String type) {
        return new Notification(
            0,
            userId,
            type,
            "Test Notification",
            "Test message for " + type,
            "/events/1",
            LocalDateTime.now(),
            null
        );
    }
}
