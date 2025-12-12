package au.edu.rmit.sept.webapp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import au.edu.rmit.sept.webapp.model.Event_Feedback;

@SpringBootTest
public class FeedbackRepositoryImplTest {

    FeedbackRepository feedbackRepo;

    @Autowired
    DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void setUp() {
        flyway.migrate();
        feedbackRepo = new FeedbackRepositoryImpl(dataSource);
    }

    @AfterEach
    public void tearDown() {
        flyway.clean();
    }

    // Helper method to create test feedback
    private Event_Feedback createTestFeedback(int eventId, int userId, int rating, String comments) {
        return new Event_Feedback(
            0, // ID will be generated
            eventId,
            userId,
            rating,
            comments,
            LocalDateTime.now()
        );
    }

    @Test
    void testSaveBasicFeedback() {
        // Arrange
        Event_Feedback feedback = createTestFeedback(1, 1, 5, "Great event!");
        
        // Act
        Event_Feedback savedFeedback = feedbackRepo.save(feedback);
        
        // Assert
        assertNotNull(savedFeedback);
        assertEquals(1, savedFeedback.event_id());
        assertEquals(1, savedFeedback.user_id());
        assertEquals(5, savedFeedback.rating());
        assertEquals("Great event!", savedFeedback.comments());
    }

    @Test
    void testFindByIdExists() {
        // Arrange
        Event_Feedback feedback = createTestFeedback(1, 1, 4, "Good event");
        Event_Feedback savedFeedback = feedbackRepo.save(feedback);
        
        // Act
        Optional<Event_Feedback> result = feedbackRepo.findById(savedFeedback.feedback_id());
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(4, result.get().rating());
    }

    @Test
    void testFindByIdNotExists() {
        // Act
        Optional<Event_Feedback> result = feedbackRepo.findById(999);
        
        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEventId() {
        // Arrange
        feedbackRepo.save(createTestFeedback(1, 1, 5, "User 1"));
        feedbackRepo.save(createTestFeedback(1, 2, 4, "User 2"));
        feedbackRepo.save(createTestFeedback(2, 1, 3, "Different event"));
        
        // Act
        List<Event_Feedback> result = feedbackRepo.findByEventId(1);
        
        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void testFindByUserId() {
        // Arrange
        feedbackRepo.save(createTestFeedback(1, 1, 5, "Event 1"));
        feedbackRepo.save(createTestFeedback(2, 1, 4, "Event 2"));
        feedbackRepo.save(createTestFeedback(1, 2, 3, "Different user"));
        
        // Act
        List<Event_Feedback> result = feedbackRepo.findByUserId(1);
        
        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void testFindByEventIdAndUserId() {
        // Arrange
        feedbackRepo.save(createTestFeedback(1, 1, 4, "Test feedback"));
        
        // Act
        Optional<Event_Feedback> result = feedbackRepo.findByEventIdAndUserId(1, 1);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(4, result.get().rating());
    }

    @Test
    void testDeleteById() {
        // Arrange
        Event_Feedback feedback = createTestFeedback(1, 1, 3, "To be deleted");
        Event_Feedback savedFeedback = feedbackRepo.save(feedback);
        
        // Act
        feedbackRepo.deleteById(savedFeedback.feedback_id());
        
        // Assert
        Optional<Event_Feedback> result = feedbackRepo.findById(savedFeedback.feedback_id());
        assertFalse(result.isPresent());
    }

    @Test
    void testGetAverageRating() {
        // Arrange
        feedbackRepo.save(createTestFeedback(1, 1, 5, "Excellent"));
        feedbackRepo.save(createTestFeedback(1, 2, 3, "Average"));
        
        // Act
        double average = feedbackRepo.getAverageRatingByEventId(1);
        
        // Assert
        assertEquals(4.0, average, 0.1); // (5+3)/2 = 4.0
    }

    @Test
    void testGetAverageRatingNoFeedback() {
        // Act
        double average = feedbackRepo.getAverageRatingByEventId(999);
        
        // Assert
        assertEquals(0.0, average, 0.1);
    }

    @Test
    void testSaveWithEmptyComments() {
        // Arrange
        Event_Feedback feedback = createTestFeedback(1, 1, 3, "");
        
        // Act
        Event_Feedback savedFeedback = feedbackRepo.save(feedback);
        
        // Assert
        assertNotNull(savedFeedback);
        assertEquals(3, savedFeedback.rating());
    }

    @Test
    void testUpdateExistingFeedback() {
        // Arrange - save initial feedback
        feedbackRepo.save(createTestFeedback(1, 1, 3, "Initial"));
        
        // Act - save updated feedback for same user/event
        Event_Feedback updated = createTestFeedback(1, 1, 5, "Updated");
        feedbackRepo.save(updated);
        
        // Assert - should be updated, not duplicated
        List<Event_Feedback> results = feedbackRepo.findByEventId(1);
        assertEquals(1, results.size());
        assertEquals(5, results.get(0).rating());
        assertEquals("Updated", results.get(0).comments());
    }
}