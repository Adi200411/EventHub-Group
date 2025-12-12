package au.edu.rmit.sept.webapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.User;

@Repository
public class UserRepositoryImpl implements UserRepository {
    
    private final DataSource dataSource;

    public UserRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery()
        ) {
            while (rs.next()) {
                LocalDateTime createdAt = rs.getTimestamp("created_at") != null 
                    ? rs.getTimestamp("created_at").toLocalDateTime() 
                    : null;
                
                User user = new User(
                    rs.getInt("user_id"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    createdAt,
                    rs.getString("status")
                );
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findAll", e);
        }
    }
    
    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setLong(1, id);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null 
                        ? rs.getTimestamp("created_at").toLocalDateTime() 
                        : null;
                    
                    User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        createdAt,
                        rs.getString("status")
                    );
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findById", e);
        }
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ?";
        
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setString(1, email);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null 
                        ? rs.getTimestamp("created_at").toLocalDateTime() 
                        : null;
                    
                    User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        createdAt,
                        rs.getString("status")
                    );
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findByEmail", e);
        }
    }

    @Override
    public Optional<User> findByUsernameAndPassword(String email, String password) {
        String sql = "SELECT * FROM Users WHERE email = ? AND password_hash = ?";
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement stm = connection.prepareStatement(sql)

        ) {
            stm.setString(1, email);
            stm.setString(2, password);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding user", e);
        }
    }

    @Override
    public User register(String email, String password) {
        // 1. First, check if a user with this email already exists.
        String checkSql = "SELECT COUNT(*) FROM Users WHERE email = ?";
        String insertSql = "INSERT INTO Users (email, password_hash) VALUES (?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            // Prepare and execute the SELECT query.
            try (PreparedStatement checkStm = connection.prepareStatement(checkSql)) {
                checkStm.setString(1, email);
                ResultSet rs = checkStm.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    // User already exists, handle this case (e.g., return null or throw a specific exception).
                    return null;
                }
            }

            // 2. If the user does not exist, proceed with the INSERT query.
            try (PreparedStatement insertStm = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStm.setString(1, email);
                insertStm.setString(2, password);
                int affectedRows = insertStm.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                // 3. Retrieve the auto-generated ID for the new user.
                try (ResultSet generatedKeys = insertStm.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        // Return the new User object with the generated ID.
                        // Note: You might need to adjust the User constructor to include the ID. Was this Gemini?
                        return new User(userId, email, password, LocalDateTime.now(),"ACTIVE");
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error registering user", e);
        }
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setLong(1, id);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in deleteById", e);
        }
    }

    @Override
    public void saveUserData(int id, String name, String course, String interest){
        String insertSql = "INSERT INTO Student_Profile (user_id, `name`, `course`, interest) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(insertSql) )
             {
                 stm.setInt(1, id);
                 stm.setString(2, name);
                 stm.setString(3, course);
                 stm.setString(4, interest);
                 stm.executeUpdate();

        }catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error registering user", e);

        }
    }

    @Override
    public java.util.Optional<au.edu.rmit.sept.webapp.model.Student_Profile> findProfileByUserId(int id) {
        String sql = "SELECT user_id, name, course, interest FROM Student_Profile WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, id);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(new au.edu.rmit.sept.webapp.model.Student_Profile(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("course"),
                            rs.getString("interest")
                    ));
                }
            }
            return java.util.Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding profile", e);
        }
    }

    @Override
    public void updateUserData(int id, String name, String course, String interest){
        String updateSql = "UPDATE Student_Profile SET `name` = ?, `course` = ?, interest = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(updateSql)) {

            stm.setString(1, name);
            stm.setString(2, course);
            stm.setString(3, interest);
            stm.setInt(4, id);

            int rows = stm.executeUpdate();
            if (rows == 0) {
                throw new DataAccessResourceFailureException("No user found with id " + id);
            }

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error updating user", e);
        }
    }
    @Override
    public void updatePassword(int id, String newHashedPassword) {
        String updateSql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(updateSql)) {
            stm.setString(1, newHashedPassword);
            stm.setInt(2, id);
            int rows = stm.executeUpdate();
            if (rows == 0) {
                throw new DataAccessResourceFailureException("No user found with id " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error updating password", e);
        }
    }
    @Override
    public void deleteProfileData(int id){
        String deleteSQL = "DELETE FROM Student_Profile WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(deleteSQL)) {
            stm.setInt(1, id);
            int rowsDeleted = stm.executeUpdate();
            if (rowsDeleted == 1) {
                System.out.println("Deleted " + rowsDeleted + " profile");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void deleteUser(int id){
        String deleteSQL = "DELETE FROM Users WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(deleteSQL)) {
            stm.setInt(1, id);
            int rowsDeleted = stm.executeUpdate();
            if (rowsDeleted == 1) {
                System.out.println("Deleted " + rowsDeleted + " User");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Optional<Organiser_Profile> getOrganiserProfileByUserId(int userId) {
        String sql = "SELECT * FROM Organiser_Profile WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, userId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                Organiser_Profile organiser = new Organiser_Profile(
                        rs.getInt("organiser_id"),
                        rs.getInt("user_id"),
                        rs.getInt("club_id"),
                        rs.getString("role_title")
                );
                return Optional.of(organiser);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Error getting organiser for user: " + userId, e);
        }
    }
        /**
     * Update user account status (ACTIVE, DEACTIVATED, or BANNED)
     */
    @Override
    public void updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET status = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setString(1, status);
            stm.setInt(2, userId);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error updating user status for id " + userId, e);
        }
    }

    /**
     * Find all users by account status
     */
    @Override
    public List<User> findByStatus(String status) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE status = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setString(1, status);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime()
                            : null;

                    users.add(new User(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            createdAt,
                            rs.getString("status")
                    )
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error fetching users by status " + status, e);
        }
        return users;
    }



    @Override
    public Optional<String> getUserRoleById(int userId) {
        String sql = """
            SELECT r.role_name 
            FROM Roles r
            JOIN User_Roles ur ON ur.role_id = r.role_id
            WHERE ur.user_id = ?
            LIMIT 1
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {

            stm.setInt(1, userId);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("role_name"));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error fetching user role for id " + userId, e);
        }
    }


    @Override
    public Optional<Integer> getOrganiserIdByUserId(int userId) {
        String sql = """
            SELECT organiser_id 
            FROM Organiser_Profile
            WHERE user_id = ?
            LIMIT 1
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {

            stm.setInt(1, userId);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("organiser_id"));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error fetching organiser ID for user id " + userId, e);
        }
    }

    @Override
    public boolean isOrganiserForEvent(int userId, int eventId) {
        String sql = """
            SELECT COUNT(*) FROM Events e 
            JOIN Organiser_Profile op ON e.organiser_id = op.organiser_id 
            WHERE op.user_id = ? AND e.event_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, userId);
            stm.setInt(2, eventId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error checking if user is organiser for event: " + e.getMessage(), e);
        }
    }

    @Override
    public void assignUserRole(int userId, int roleId) {
        String sql = "INSERT INTO User_Roles (user_id, role_id) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, userId);
            stm.setInt(2, roleId);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error assigning role to user", e);
        }
    }
}