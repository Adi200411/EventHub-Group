package au.edu.rmit.sept.webapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataAccessResourceFailureException;

import au.edu.rmit.sept.webapp.model.User;

/*
 * This test class is disabled due to Mockito DataSource mocking issues with Java 24.
 * The AdminUserRepositoryImpl class should have integration tests or be tested through higher-level tests.
 */
//@Disabled
class AdminUserRepositoryImplTests {

    
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private AdminUserRepositoryImpl repo;

    @BeforeEach
    void setup() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        repo = new AdminUserRepositoryImpl(dataSource);
    }

    // === findAllUsers ===

    @Test
    void findAllUsersReturnsListOfUsers() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("user_id")).thenReturn(1);
        when(resultSet.getString("email")).thenReturn("test@example.com");
        when(resultSet.getString("password_hash")).thenReturn("hash");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2025, 10, 10, 10, 0)));
        when(resultSet.getString("status")).thenReturn("ACTIVE");

        List<User> users = repo.findAllUsers();
        assertEquals(1, users.size());
        assertEquals("test@example.com", users.get(0).email());
        verify(statement, times(1)).executeQuery();
    }

    @Test
    void findAllUsersThrowsOnSqlError() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB down"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findAllUsers());
    }

    // === findUserById ===

    @Test
    void findUserByIdReturnsUserWhenFound() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("user_id")).thenReturn(1);
        when(resultSet.getString("email")).thenReturn("bob@example.com");
        when(resultSet.getString("password_hash")).thenReturn("hash");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getString("status")).thenReturn("ACTIVE");

        Optional<User> user = repo.findUserById(1L);
        assertTrue(user.isPresent());
        assertEquals("bob@example.com", user.get().email());
        verify(statement).setLong(1, 1L);
    }

    @Test
    void findUserByIdReturnsEmptyWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> user = repo.findUserById(99L);
        assertTrue(user.isEmpty());
    }

    @Test
    void findUserByIdThrowsOnSqlException() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findUserById(1L));
    }

    // === updateUserStatus ===

    @Test
    void updateUserStatusReturnsTrueWhenRowsUpdated() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        boolean result = repo.updateUserStatus(10L, "BANNED");
        assertTrue(result);

        InOrder order = inOrder(statement);
        order.verify(statement).setString(1, "BANNED");
        order.verify(statement).setLong(2, 10L);
    }

    @Test
    void updateUserStatusReturnsFalseWhenNoRowsUpdated() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);

        boolean result = repo.updateUserStatus(1L, "ACTIVE");
        assertFalse(result);
    }

    @Test
    void updateUserStatusThrowsOnSqlError() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("bad"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.updateUserStatus(1L, "ACTIVE"));
    }

    // === findUsersByStatus ===

    @Test
    void findUsersByStatusReturnsUsersList() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        when(resultSet.getInt("user_id")).thenReturn(2);
        when(resultSet.getString("email")).thenReturn("x@y.com");
        when(resultSet.getString("password_hash")).thenReturn("hash");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getString("status")).thenReturn("BANNED");

        List<User> users = repo.findUsersByStatus("BANNED");

        assertEquals(1, users.size());
        assertEquals("x@y.com", users.get(0).email());
        verify(statement).setString(1, "BANNED");
    }

    @Test
    void findUsersByStatusThrowsOnSqlError() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("no connection"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findUsersByStatus("ACTIVE"));
    }

    // === resource cleanup verification ===

    @Test
    void allResourcesClosedProperlyInFindAllUsers() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        repo.findAllUsers();

        verify(resultSet, times(1)).close();
        verify(statement, times(1)).close();
        verify(connection, times(1)).close();
    }
}
