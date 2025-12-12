package au.edu.rmit.sept.webapp.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class EventRepositoryImplTest {

    // --- Integration Setup ---
    @Autowired private DataSource realDataSource;
    @Autowired private Flyway flyway;
    private EventRepository realRepo;

    // --- Mock Setup ---
    @Mock private DataSource mockDataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private JdbcTemplate mockJdbcTemplate;
    
    private EventRepositoryImpl repo; // Mock repository for unit tests

    @BeforeEach
    void setUp() throws SQLException {
        // Setup integration tests
        flyway.migrate();
        realRepo = new EventRepositoryImpl(realDataSource, new JdbcTemplate(realDataSource));
    }
    
    void setupMocks() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(connection);
        
        // Initialize the mock repository for unit tests
        repo = new EventRepositoryImpl(mockDataSource, mockJdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        flyway.clean();
    }

    // === Existing Integration Tests ===
    @Test
    void listAllEvents_should_returnEvents() {
        List<Event> events = realRepo.listAllEvents();
        assertFalse(events.isEmpty());
    }

    @Test
    void findUpcomingEvents_should_returnOnlyUpcomingEvents() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
        List<Event> upcomingEvents = realRepo.findUpcomingEvents(now);
        assertFalse(upcomingEvents.isEmpty());
        assertTrue(upcomingEvents.stream().allMatch(e ->
                !e.date().isBefore(now.toLocalDate())));
    }

    @Test
    void findPastEvents_should_returnOnlyPastEvents() {
        LocalDateTime year_end = LocalDateTime.of(2025, 12, 31, 0, 0);
        List<Event> pastEvents = realRepo.findPastEvents(year_end);
        assertFalse(pastEvents.isEmpty());
        assertTrue(pastEvents.stream().allMatch(e ->
                e.date().isBefore(year_end.toLocalDate())));
    }

    @Test
    void searchEvents_should_filterByQuery() {
        List<Event> events = realRepo.searchEvents("Tech", null, null, true, LocalDateTime.now());
        assertFalse(events.isEmpty());
    }



    
    @Test
    void findRsvpedEventIds_returnsIdsSuccessfully() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt("event_id")).thenReturn(1, 2);

        List<Integer> result = repo.findRsvpedEventIds(5L);

        assertEquals(List.of(1, 2), result);
        verify(ps).setLong(1, 5L);
    }

    @Test
    void findRsvpedEventIds_throwsDataAccessOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("boom"));
        assertThrows(DataAccessResourceFailureException.class,
                () -> repo.findRsvpedEventIds(9L));
    }

    @Test
    void rsvp_insertsSuccessfully() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        int result = repo.rsvp(2L, 7L);

        assertEquals(1, result);
        verify(ps).setLong(1, 2L);
        verify(ps).setLong(2, 7L);
    }

    @Test
    void rsvp_throwsExceptionOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("fail"));
        assertThrows(DataAccessResourceFailureException.class,
                () -> repo.rsvp(3L, 8L));
    }

    @Test
    void findById_returnsEventWhenFound() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("event_id")).thenReturn(1);
        when(rs.getObject("organiser_id", Long.class)).thenReturn(2L);
        when(rs.getString("title")).thenReturn("Mock Event");
        when(rs.getString("description")).thenReturn("Mock Description");
        when(rs.getString("location")).thenReturn("Mock Location");
        when(rs.getDate("date")).thenReturn(Date.valueOf("2025-10-10"));
        when(rs.getTimestamp("start_time")).thenReturn(Timestamp.valueOf("2025-10-10 10:00:00"));
        when(rs.getString("finish_time")).thenReturn("11:00");
        when(rs.getString("status")).thenReturn("ACTIVE");
        when(rs.getInt("capacity")).thenReturn(100);

        Optional<Event> result = repo.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Mock Event", result.get().title());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(repo.findById(999L).isEmpty());
    }

    @Test
    void findById_throwsExceptionOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("boom"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findById(9L));
    }

    @Test
    void insertEvent_returnsGeneratedId() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any(), anyInt())).thenReturn(ps);
        ResultSet keySet = mock(ResultSet.class);
        when(ps.getGeneratedKeys()).thenReturn(keySet);
        when(keySet.next()).thenReturn(true);
        when(keySet.getInt(1)).thenReturn(99);

        Event e = new Event(0, 2L, 3L, "title", "desc", "loc",
                LocalDate.now(), LocalDateTime.now(), "22:00", "ACTIVE", 100);

        int id = repo.insertEvent(e);
        assertEquals(99, id);
        verify(ps).executeUpdate();
    }

    @Test
    void insertEvent_throwsExceptionOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any(), anyInt())).thenThrow(new SQLException("fail"));
        Event e = new Event(0, 2L, 3L, "t", "d", "l", LocalDate.now(), LocalDateTime.now(), "22:00", "ACTIVE", 5);
        assertThrows(DataAccessResourceFailureException.class, () -> repo.insertEvent(e));
    }

    @Test
    void updateEvent_executesSuccessfully() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeUpdate()).thenReturn(1);

        Event e = new Event(1, 2L, 3L, "title", "desc", "loc",
                LocalDate.now(), LocalDateTime.now(), "22:00", "ACTIVE", 100);
        int result = repo.updateEvent(e);

        assertEquals(1, result);
    }

    @Test
    void updateEvent_throwsOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("fail"));
        Event e = new Event(1, 2L, 3L, "t", "d", "l", LocalDate.now(), LocalDateTime.now(), "22:00", "ACTIVE", 5);
        assertThrows(DataAccessResourceFailureException.class, () -> repo.updateEvent(e));
    }

    @Test
    void cancelEventById_updatesSuccessfully() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        int result = repo.cancelEventById(5L);

        assertEquals(1, result);
        verify(ps).setLong(1, 5L);
    }

    @Test
    void cancelEventById_throwsOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("bad"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.cancelEventById(5L));
    }

    @Test
    void deleteEventById_successful() throws Exception {
        setupMocks();
        PreparedStatement ps1 = mock(PreparedStatement.class);
        PreparedStatement ps2 = mock(PreparedStatement.class);

        when(connection.prepareStatement("UPDATE Events SET status = 'DELETED' WHERE event_id = ?")).thenReturn(ps1);
        when(connection.prepareStatement("INSERT INTO Deleted_Events_Log (event_id, admin_id, reason) VALUES (?, ?, ?)"))
                .thenReturn(ps2);

        int result = repo.deleteEventById(5, 9L, "test reason");

        assertEquals(1, result);
        verify(ps1).executeUpdate();
        verify(ps2).executeUpdate();
    }

    @Test
    void deleteEventById_throwsOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("fail"));
        assertThrows(DataAccessResourceFailureException.class,
                () -> repo.deleteEventById(1, 2L, "reason"));
    }

    @Test
    void findDeletedEvents_returnsEvents() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("event_id")).thenReturn(10);
        when(rs.getObject("organiser_id", Long.class)).thenReturn(2L);
        when(rs.getString("title")).thenReturn("Deleted");
        when(rs.getString("description")).thenReturn("Deleted Description");
        when(rs.getString("location")).thenReturn("Deleted Location");
        when(rs.getDate("date")).thenReturn(Date.valueOf("2025-10-01"));
        when(rs.getTimestamp("start_time")).thenReturn(Timestamp.valueOf("2025-10-01 10:00:00"));
        when(rs.getString("finish_time")).thenReturn("11:00");
        when(rs.getString("status")).thenReturn("DELETED");
        when(rs.getInt("capacity")).thenReturn(100);

        List<Event> result = repo.findDeletedEvents();

        assertEquals(1, result.size());
        assertEquals("Deleted", result.get(0).title());
    }

    @Test
    void findDeletedEvents_throwsOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("boom"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findDeletedEvents());
    }

    @Test
    void findDeletedLogs_returnsLogs() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("log_id")).thenReturn(1);
        when(rs.getInt("event_id")).thenReturn(2);
        when(rs.getLong("admin_id")).thenReturn(3L);
        when(rs.getString("reason")).thenReturn("test");
        when(rs.getTimestamp("deleted_at")).thenReturn(Timestamp.valueOf("2025-10-10 10:00:00"));

        List<DeletedEventLog> logs = repo.findDeletedLogs();

        assertEquals(1, logs.size());
        assertEquals("test", logs.get(0).reason());
    }

    @Test
    void findDeletedLogs_throwsOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("bad"));
        assertThrows(DataAccessResourceFailureException.class, () -> repo.findDeletedLogs());
    }

    @Test
    void searchUpcomingEnhanced_returnsEmptyList() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Event> result = repo.searchUpcomingEnhanced("test", LocalDate.now(), LocalDateTime.now(), 5, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchUpcomingEnhanced_throwsRuntimeOnSqlError() throws Exception {
        setupMocks();
        when(connection.prepareStatement(any())).thenThrow(new SQLException("boom"));
        assertThrows(RuntimeException.class,
                () -> repo.searchUpcomingEnhanced(null, null, null, 1, 0));
    }
    
}
