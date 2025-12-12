package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Notification;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final DataSource dataSource;

    public NotificationRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Notification save(Notification notification) {
        String sql = "INSERT INTO Notifications (user_id, type, title, message, link, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, notification.user_id());
            stmt.setString(2, notification.type());
            stmt.setString(3, notification.title());
            stmt.setString(4, notification.message());
            stmt.setString(5, notification.link());
            stmt.setTimestamp(6, Timestamp.valueOf(notification.created_at() != null ? 
                notification.created_at() : LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new DataAccessResourceFailureException("Creating notification failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    return new Notification(
                        newId,
                        notification.user_id(),
                        notification.type(),
                        notification.title(),
                        notification.message(),
                        notification.link(),
                        notification.created_at() != null ? notification.created_at() : LocalDateTime.now(),
                        null
                    );
                } else {
                    throw new DataAccessResourceFailureException("Creating notification failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error saving notification", e);
        }
    }

    @Override
    public Optional<Notification> findById(int notificationId) {
        String sql = "SELECT * FROM Notifications WHERE notification_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, notificationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding notification by ID", e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Notification> findByUserId(int userId) {
        String sql = "SELECT * FROM Notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding notifications by user ID", e);
        }
        
        return notifications;
    }

    @Override
    public List<Notification> findUnreadByUserId(int userId) {
        String sql = "SELECT * FROM Notifications WHERE user_id = ? AND read_at IS NULL ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding unread notifications", e);
        }
        
        return notifications;
    }

    @Override
    public void markAsRead(int notificationId) {
        String sql = "UPDATE Notifications SET read_at = ? WHERE notification_id = ? AND read_at IS NULL";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, notificationId);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error marking notification as read", e);
        }
    }

    @Override
    public void markAllAsReadForUser(int userId) {
        String sql = "UPDATE Notifications SET read_at = ? WHERE user_id = ? AND read_at IS NULL";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error marking all notifications as read", e);
        }
    }

    @Override
    public void deleteById(int notificationId) {
        String sql = "DELETE FROM Notifications WHERE notification_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error deleting notification", e);
        }
    }

    @Override
    public void deleteAllForUser(int userId) {
        String sql = "DELETE FROM Notifications WHERE user_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error deleting all notifications for user", e);
        }
    }

    @Override
    public int countUnreadByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE user_id = ? AND read_at IS NULL";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error counting unread notifications", e);
        }
        
        return 0;
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Timestamp readAtTimestamp = rs.getTimestamp("read_at");
        LocalDateTime readAt = readAtTimestamp != null ? readAtTimestamp.toLocalDateTime() : null;
        
        return new Notification(
            rs.getInt("notification_id"),
            rs.getInt("user_id"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("link"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            readAt
        );
    }
}
