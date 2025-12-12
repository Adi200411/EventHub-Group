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

import au.edu.rmit.sept.webapp.model.Event_Photos;

@Repository
public class PhotoRepositoryImpl implements PhotoRepository {

    private final DataSource dataSource;

    public PhotoRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Event_Photos save(Event_Photos photo) {
        String insertSql = """
            INSERT INTO Event_Photos (event_id, organiser_id, url, uploaded_at)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            
            stm.setInt(1, photo.event_id());
            stm.setInt(2, photo.organiser_id());
            stm.setString(3, photo.url());
            stm.setTimestamp(4, Timestamp.valueOf(photo.uploaded_at()));
            
            int affectedRows = stm.executeUpdate();
            
            if (affectedRows == 0) {
                throw new DataAccessResourceFailureException("Creating photo failed, no rows affected");
            }
            
            try (ResultSet generatedKeys = stm.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int photoId = generatedKeys.getInt(1);
                    return new Event_Photos(
                        photoId,
                        photo.event_id(),
                        photo.organiser_id(),
                        photo.url(),
                        photo.uploaded_at()
                    );
                } else {
                    throw new DataAccessResourceFailureException("Failed to get generated photo ID");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error saving photo", e);
        }
    }

    @Override
    public Optional<Event_Photos> findById(int photoId) {
        String sql = "SELECT * FROM Event_Photos WHERE photo_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, photoId);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPhoto(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding photo by ID", e);
        }
    }

    @Override
    public List<Event_Photos> findByEventId(int eventId) {
        String sql = "SELECT * FROM Event_Photos WHERE event_id = ? ORDER BY uploaded_at DESC";
        List<Event_Photos> photos = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, eventId);
            
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    photos.add(mapRowToPhoto(rs));
                }
                return photos;
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding photos by event ID", e);
        }
    }

    @Override
    public List<Event_Photos> findByOrganiserId(int organiserId) {
        String sql = "SELECT * FROM Event_Photos WHERE organiser_id = ? ORDER BY uploaded_at DESC";
        List<Event_Photos> photos = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, organiserId);
            
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    photos.add(mapRowToPhoto(rs));
                }
                return photos;
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding photos by organiser ID", e);
        }
    }

    @Override
    public void deleteById(int photoId) {
        String sql = "DELETE FROM Event_Photos WHERE photo_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            stm.setInt(1, photoId);
            stm.executeUpdate();
            
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error deleting photo", e);
        }
    }

    @Override
    public void deleteMultipleById(List<Integer> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM Event_Photos WHERE photo_id IN (" + 
                     String.join(",", photoIds.stream().map(id -> "?").toList()) + ")";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            
            for (int i = 0; i < photoIds.size(); i++) {
                stm.setInt(i + 1, photoIds.get(i));
            }
            stm.executeUpdate();
            
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error deleting multiple photos", e);
        }
    }

    @Override
    public List<Event_Photos> findAll() {
        String sql = "SELECT * FROM Event_Photos ORDER BY uploaded_at DESC";
        List<Event_Photos> photos = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            
            while (rs.next()) {
                photos.add(mapRowToPhoto(rs));
            }
            return photos;
            
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding all photos", e);
        }
    }

    private Event_Photos mapRowToPhoto(ResultSet rs) throws SQLException {
        return new Event_Photos(
            rs.getInt("photo_id"),
            rs.getInt("event_id"),
            rs.getInt("organiser_id"),
            rs.getString("url"),
            rs.getTimestamp("uploaded_at").toLocalDateTime()
        );
    }
}