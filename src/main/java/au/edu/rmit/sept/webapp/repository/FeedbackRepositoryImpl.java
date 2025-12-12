package au.edu.rmit.sept.webapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import au.edu.rmit.sept.webapp.model.Event_Feedback;

@Repository
public class FeedbackRepositoryImpl implements FeedbackRepository {

    private final DataSource dataSource;

    public FeedbackRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Event_Feedback save(Event_Feedback feedback) {
        // First, ensure the user has an attendance record for this event
        // This is required by the foreign key constraint in the schema
        String attendanceCheckSql = """
            INSERT IGNORE INTO Attendance (event_id, user_id, checkin_time)
            VALUES (?, ?, NOW())
            """;

        try (Connection connection = dataSource.getConnection()) {
            // First ensure attendance record exists
            try (PreparedStatement attendanceStm = connection.prepareStatement(attendanceCheckSql)) {
                attendanceStm.setInt(1, feedback.event_id());
                attendanceStm.setInt(2, feedback.user_id());
                attendanceStm.executeUpdate();
            }
            
            // Check if feedback already exists for this user and event
            Optional<Event_Feedback> existingFeedback = findByEventIdAndUserId(feedback.event_id(), feedback.user_id());
            
            if (existingFeedback.isPresent()) {
                // Update existing feedback
                String updateSql = """
                    UPDATE Event_Feedback 
                    SET rating = ?, comments = ?, submitted_at = ?
                    WHERE event_id = ? AND user_id = ?
                    """;
                    
                try (PreparedStatement updateStm = connection.prepareStatement(updateSql)) {
                    updateStm.setInt(1, feedback.rating());
                    updateStm.setString(2, feedback.comments());
                    updateStm.setTimestamp(3, Timestamp.valueOf(feedback.feedback_date()));
                    updateStm.setInt(4, feedback.event_id());
                    updateStm.setInt(5, feedback.user_id());
                    
                    updateStm.executeUpdate();
                    
                    // Return the updated feedback with the existing ID
                    return new Event_Feedback(
                        existingFeedback.get().feedback_id(),
                        feedback.event_id(),
                        feedback.user_id(),
                        feedback.rating(),
                        feedback.comments(),
                        feedback.feedback_date()
                    );
                }
            } else {
                // Insert new feedback
                String insertSql = """
                    INSERT INTO Event_Feedback (event_id, user_id, rating, comments, submitted_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
                    
                try (PreparedStatement insertStm = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStm.setInt(1, feedback.event_id());
                    insertStm.setInt(2, feedback.user_id());
                    insertStm.setInt(3, feedback.rating());
                    insertStm.setString(4, feedback.comments());
                    insertStm.setTimestamp(5, Timestamp.valueOf(feedback.feedback_date()));
                    
                    insertStm.executeUpdate();
                    
                    try (ResultSet generatedKeys = insertStm.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int feedbackId = generatedKeys.getInt(1);
                            return new Event_Feedback(
                                feedbackId,
                                feedback.event_id(),
                                feedback.user_id(),
                                feedback.rating(),
                                feedback.comments(),
                                feedback.feedback_date()
                            );
                        } else {
                            throw new DataAccessResourceFailureException("Failed to get generated feedback ID");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error saving feedback", e);
        }
    }

    @Override
    public Optional<Event_Feedback> findById(int feedbackId) {
        String sql = "SELECT * FROM Event_Feedback WHERE feedback_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, feedbackId);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFeedback(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding feedback by ID", e);
        }
    }

    @Override
    public List<Event_Feedback> findByEventId(int eventId) {
        List<Event_Feedback> feedbacks = new ArrayList<>();
        String sql = "SELECT * FROM Event_Feedback WHERE event_id = ? ORDER BY submitted_at DESC";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, eventId);
            
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    feedbacks.add(mapRowToFeedback(rs));
                }
            }
            return feedbacks;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding feedback by event ID", e);
        }
    }

    @Override
    public List<Event_Feedback> findByUserId(int userId) {
        List<Event_Feedback> feedbacks = new ArrayList<>();
        String sql = "SELECT * FROM Event_Feedback WHERE user_id = ? ORDER BY submitted_at DESC";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, userId);
            
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    feedbacks.add(mapRowToFeedback(rs));
                }
            }
            return feedbacks;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding feedback by user ID", e);
        }
    }

    @Override
    public Optional<Event_Feedback> findByEventIdAndUserId(int eventId, int userId) {
        String sql = "SELECT * FROM Event_Feedback WHERE event_id = ? AND user_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, eventId);
            stm.setInt(2, userId);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFeedback(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding feedback by event and user ID", e);
        }
    }

    @Override
    public void deleteById(int feedbackId) {
        String sql = "DELETE FROM Event_Feedback WHERE feedback_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, feedbackId);
            stm.executeUpdate();
            
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error deleting feedback", e);
        }
    }

    @Override
    public double getAverageRatingByEventId(int eventId) {
        String sql = "SELECT AVG(rating) as average_rating FROM Event_Feedback WHERE event_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, eventId);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("average_rating");
                }
                return 0.0;
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error calculating average rating", e);
        }
    }

    private Event_Feedback mapRowToFeedback(ResultSet rs) throws SQLException {
        return new Event_Feedback(
            rs.getInt("feedback_id"),
            rs.getInt("event_id"),
            rs.getInt("user_id"),
            rs.getInt("rating"),
            rs.getString("comments"),
            rs.getTimestamp("submitted_at").toLocalDateTime()
        );
    }
}