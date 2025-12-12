package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.User;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminUserRepositoryImpl implements AdminUserRepository {

    private final DataSource dataSource;

    public AdminUserRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = """
            SELECT user_id, email, password_hash, created_at, status
            FROM Users
            ORDER BY user_id;
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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
            throw new DataAccessResourceFailureException("Error fetching all users", e);
        }
    }

    @Override
    public Optional<User> findUserById(Long id) {
        String sql = """
            SELECT user_id, email, password_hash, created_at, status
            FROM Users
            WHERE user_id = ?;
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
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
            throw new DataAccessResourceFailureException("Error fetching user by ID " + id, e);
        }
    }

    @Override
    public boolean updateUserStatus(Long userId, String status) {
        String sql = """
            UPDATE Users
            SET status = ?
            WHERE user_id = ?;
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error updating user status to " + status, e);
        }
    }


    @Override
    public List<User> findUsersByStatus(String status) {
        List<User> users = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.email, u.password_hash, u.created_at,
                COALESCE(us.status, 'ACTIVE') AS status
            FROM Users u
            LEFT JOIN User_Status us ON u.user_id = us.user_id
            WHERE COALESCE(us.status, 'ACTIVE') = ?
            ORDER BY u.user_id;
            """;

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
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
            }

            return users;

        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error fetching users by status " + status, e);
        }
    }

}
