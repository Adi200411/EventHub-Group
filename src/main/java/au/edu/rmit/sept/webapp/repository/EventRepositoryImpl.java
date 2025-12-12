package au.edu.rmit.sept.webapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import au.edu.rmit.sept.webapp.dto.RsvpUserDetail;
import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Tags;

@Repository
public class EventRepositoryImpl implements EventRepository {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    public EventRepositoryImpl(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Event> findRsvpedEventsByUserId(Long userId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.* FROM Events e JOIN RSVP r ON e.event_id = r.event_id WHERE r.user_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {

            stm.setLong(1, userId);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                events.add(mapEventRowCompat(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding RSVP'd events for user " + userId, e);
        }
        return events;
    }

    @Override
    public List<Event> findPastEventsAttendedByUser(Long userId, LocalDateTime currentDate) {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT e.*
            FROM Events e
            JOIN RSVP r ON e.event_id = r.event_id
            WHERE e.date < ?
            AND r.user_id = ?
            AND e.status <> 'DELETED'
            ORDER BY e.date DESC
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(sql)) {

            stm.setObject(1, currentDate.toLocalDate());
            stm.setLong(2, userId);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                events.add(mapEventRowCompat(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error finding past events attended by user " + userId, e);
        }
        return events;
    }

    @Override
    public boolean checkInUser(Long eventId, Long userId) {
        //check if the user has RSVP'd
        String sqlCheck = "SELECT COUNT(*) FROM RSVP WHERE event_id = ? AND user_id = ?";
        String sqlInsert = "INSERT INTO Attendance (event_id, user_id, checkin_time) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE checkin_time = NOW()";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement checkStm = connection.prepareStatement(sqlCheck)) {
                checkStm.setLong(1, eventId);
                checkStm.setLong(2, userId);
                ResultSet rs = checkStm.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    try (PreparedStatement insertStm = connection.prepareStatement(sqlInsert)) {
                        insertStm.setLong(1, eventId);
                        insertStm.setLong(2, userId);
                        int affectedRows = insertStm.executeUpdate();
                        return affectedRows > 0;
                    }
                } else {
                    return false; //not rsvped
                }
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error checking in user", e);
        }
    }

    @Override
    public List<RsvpUserDetail> findRsvpDetailsByEventId(Long eventId) {
        final String sql = """
            SELECT
                R.user_id,
                U.email,
                SP.name,
                SP.course,
                R.rsvp_date,
                R.qr_code
            FROM RSVP R
            JOIN Users U ON R.user_id = U.user_id
            LEFT JOIN Student_Profile SP ON R.user_id = SP.user_id
            WHERE R.event_id = ?
            ORDER BY R.rsvp_date ASC
            """;

        return jdbcTemplate.query(sql, new Object[]{eventId}, new RsvpUserDetailRowMapper());
    }

    // Helper class definition must be included within the file
    private static class RsvpUserDetailRowMapper implements RowMapper<RsvpUserDetail> {
        @Override
        public RsvpUserDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RsvpUserDetail(
                rs.getLong("user_id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("course"),
                rs.getTimestamp("rsvp_date").toLocalDateTime(),
                rs.getString("qr_code")
            );
        }
    }

    @Override
    public List<Event> listAllEvents() {
        List<Event> events = new ArrayList<>();
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Events WHERE status <> 'DELETED';");
            ResultSet rs = stm.executeQuery()
        ) {
            while (rs.next()) {
                LocalDate eventDate = rs.getDate("date").toLocalDate();
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
                Long organiserId   = rs.getObject("organiser_id", Long.class);
                Long clubId   = rs.getObject("club_id", Long.class);

                Event e = new Event(
                    rs.getInt("event_id"),
                    organiserId,
                    clubId,
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("location"),
                    eventDate,
                    startDateTime,
                    rs.getTime("finish_time").toString(),
                    rs.getString("status"),
                    rs.getInt("capacity")
                );
                events.add(e);
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in listAllEvents", e);
        }
    }

    // In au.edu.rmit.sept.webapp.repository.EventRepositoryImpl.java

   // In your EventRepositoryImpl.java

    @Override
    public List<Event> findRecommendedBySimilarity(Long userId, LocalDateTime now, int limit) {
        List<Event> events = new ArrayList<>();

        final String sql = """
            WITH user_tag_names AS (
                SELECT DISTINCT LOWER(t.tag_name) AS tag_name
                FROM RSVP r
                JOIN Event_Tags et ON et.event_id = r.event_id
                JOIN Tags t ON t.tag_id = et.tag_id
                WHERE r.user_id = ? 
                UNION DISTINCT
                SELECT LOWER(TRIM(sp.interest)) AS tag_name
                FROM Student_Profile sp
                WHERE sp.user_id = ? AND sp.interest IS NOT NULL AND TRIM(sp.interest) <> ''
            ),
            user_tag_count AS (
                SELECT GREATEST(COUNT(*), 1) AS c FROM user_tag_names
            ),
            event_tag_names AS (
                SELECT et.event_id, LOWER(t.tag_name) AS tag_name
                FROM Event_Tags et
                JOIN Tags t ON t.tag_id = et.tag_id
            ),
            event_tag_counts AS (
                SELECT event_id, COUNT(DISTINCT tag_name) AS c
                FROM event_tag_names
                GROUP BY event_id
            ),
            intersections AS (
                SELECT etn.event_id, COUNT(DISTINCT etn.tag_name) AS shared_count
                FROM event_tag_names etn
                JOIN user_tag_names utn 
                ON etn.tag_name LIKE CONCAT('%', utn.tag_name, '%')
                OR utn.tag_name LIKE CONCAT('%', etn.tag_name, '%') 
                GROUP BY etn.event_id
            )
            SELECT e.*,
                    COALESCE(i.shared_count / SQRT(utc.c * etc.c), 0) AS similarity_score
            FROM events e
            LEFT JOIN event_tag_counts etc ON etc.event_id = e.event_id
            LEFT JOIN intersections i      ON i.event_id   = e.event_id
            CROSS JOIN user_tag_count utc
            LEFT JOIN RSVP rself ON rself.event_id = e.event_id AND rself.user_id = ?
            WHERE rself.event_id IS NULL
            AND LOWER(e.status) = 'active'
            AND (
                e.date > DATE(?)
                OR (e.date = DATE(?) AND e.start_time >= TIME(?))
            )
            AND COALESCE(i.shared_count, 0) > 0 
            ORDER BY similarity_score DESC, e.date ASC, e.start_time ASC
            LIMIT ?
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            
            stm.setLong(1, userId);
            stm.setLong(2, userId);
            stm.setLong(3, userId);
            stm.setObject(4, now.toLocalDate());
            stm.setObject(5, now.toLocalDate());
            stm.setObject(6, now.toLocalTime());
            stm.setInt(7, limit);
                
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    LocalDate eventDate = rs.getDate("date").toLocalDate();
                    LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
                    
                    Long organiserId = rs.getObject("organiser_id", Long.class);
                    Long clubId = rs.getObject("club_id", Long.class);

                    Event e = new Event(
                        rs.getInt("event_id"), 
                        organiserId,
                        clubId,
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        eventDate,
                        startDateTime,
                        rs.getTime("finish_time").toString(),
                        rs.getString("status"),
                        rs.getInt("capacity")
                    );
                    events.add(e);
                }
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findRecommendedBySimilarity", e);
        }
    }


    @Override
    public List<Integer> findRsvpedEventIds(Long userId) {
        List<Integer> eventIds = new ArrayList<>();
        String sql = "SELECT event_id FROM RSVP WHERE user_id = ?";

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setLong(1, userId);

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    eventIds.add(rs.getInt("event_id"));
                }
            }
            return eventIds;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findRsvpedEventIds", e);
        }
    }

    @Override
    public int rsvp(Long eventId, Long userId) {
        String sql = """
            INSERT INTO RSVP (event_id, user_id, rsvp_date, qr_code)
            VALUES (?, ?, NOW(), UUID())
            ON DUPLICATE KEY UPDATE rsvp_date = VALUES(rsvp_date)
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setLong(1, eventId);
            stm.setLong(2, userId);

            return stm.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in rsvp", e);
        }
    }

    @Override
    public int cancelRsvp(Long eventId, Long userId) {
        String sql = "DELETE FROM RSVP WHERE event_id = ? AND user_id = ?";
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setLong(1, eventId);
            stm.setLong(2, userId);
            return stm.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in cancelRsvp", e);
        }
    }

    @Override
    public List<Event> findUpcomingEvents(LocalDateTime currentDate) {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT *
            FROM Events
            WHERE date >= ?
            AND status = 'ACTIVE'
            ORDER BY date ASC
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sql)
        ) {
            stm.setObject(1, currentDate.toLocalDate()); // or setTimestamp if column is DATETIME

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    LocalDate eventDate = rs.getDate("date").toLocalDate();
                    LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
                    Long organiserId   = rs.getObject("organiser_id", Long.class);
                    Long clubId   = rs.getObject("club_id", Long.class);

                    Event e = new Event(
                        rs.getInt("event_id"),
                        organiserId,
                        clubId,
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        eventDate,
                        startDateTime,
                        rs.getTime("finish_time").toString(),
                        rs.getString("status"),
                        rs.getInt("capacity")
                    );
                    events.add(e);
                }
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in findUpcomingEvents", e);
        }
    }

    @Override
    public List<Event> findPastEvents(LocalDateTime currentDate) {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT *
            FROM Events
            WHERE date < ?
            AND status <> 'DELETED'
            ORDER BY date DESC
            """;

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statementCon = connection.prepareStatement(sql)
        ) {
            statementCon.setObject(1, currentDate.toLocalDate());

            try (ResultSet rs = statementCon.executeQuery()) {
                while (rs.next()) {
                    LocalDate eventDate = rs.getDate("date").toLocalDate();
                    LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
                    Long organiserId   = rs.getObject("organiser_id", Long.class);
                    Long clubId   = rs.getObject("club_id", Long.class);

                    Event e = new Event(
                        rs.getInt("event_id"),
                        organiserId,
                        clubId,
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        eventDate,
                        startDateTime,
                        rs.getTime("finish_time").toString(),
                        rs.getString("status"),
                        rs.getInt("capacity")
                    );
                    events.add(e);
                }
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in finding Past Events", e);
        }
    }

    @Override
    public List<Event> searchEvents(String query, String tag, LocalDate date, boolean isUpcoming, LocalDateTime now) {
        List<Event> events = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        //Sql queries assited by LLM
        StringBuilder sqlQuery = new StringBuilder("""
            SELECT e.* FROM Events e
            LEFT JOIN Event_Tags et ON e.event_id = et.event_id
            LEFT JOIN Tags t ON et.tag_id = t.tag_id
            WHERE 1=1
            """);

        if (query != null && !query.trim().isEmpty()) {
            sqlQuery.append(" AND LOWER(e.title) LIKE LOWER(?)");
            params.add("%" + query + "%");
        }

        if (tag != null && !"All Tags".equalsIgnoreCase(tag)) {
            sqlQuery.append(" AND LOWER(t.tag_name) = LOWER(?)");
            params.add(tag);
        }

        if (date != null) {
            sqlQuery.append(" AND e.date = ?");
            params.add(date);
        }

        if (isUpcoming) {
            sqlQuery.append(" AND e.date >= ?");
            params.add(now.toLocalDate());
        } else {
            sqlQuery.append(" AND e.date < ?");
            params.add(now.toLocalDate());
        }
        sqlQuery.append(" AND e.status <> 'DELETED' ");
        sqlQuery.append(" GROUP BY e.event_id ORDER BY e.date DESC");

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement stm = connection.prepareStatement(sqlQuery.toString())
        ) {
            for (int i = 0; i < params.size(); i++) {
                stm.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    LocalDate eventDate = rs.getDate("date").toLocalDate();
                    LocalTime startTime = rs.getTime("start_time").toLocalTime();
                    LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
                    Long organiserId   = rs.getObject("organiser_id", Long.class);
                    Long clubId   = rs.getObject("club_id", Long.class);

                    Event e = new Event(
                        rs.getInt("event_id"),
                        organiserId,
                        clubId,
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        eventDate,
                        startDateTime,
                        rs.getTime("finish_time").toString(),
                        rs.getString("status"),
                        rs.getInt("capacity")
                    );
                    events.add(e);
                }
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in searchEvents", e);
        }
    }


    // === ADD-ONLY: Implementation of searchUpcomingEnhanced (SQL filters + optional LIMIT/OFFSET) ===

@Override
public java.util.List<au.edu.rmit.sept.webapp.model.Event> searchUpcomingEnhanced(
        String q,
        java.time.LocalDate today,
        java.time.LocalDateTime now,
        java.lang.Integer limit,
        java.lang.Integer offset
) {
    final java.time.LocalDate useToday = (today != null ? today : java.time.LocalDate.now());
    final java.time.LocalDateTime useNow = (now != null ? now : java.time.LocalDateTime.now());

    // Build WHERE with your legacy semantics:
    //   (date > today) OR (date = today AND start_time >= now)
    //   AND (status IS NULL OR UPPER(status) <> 'CANCELLED')
    //   AND q-match across title/description/location (if q provided)
    final StringBuilder sb = new StringBuilder();
    sb.append("""
        SELECT e.event_id
          FROM Events e
        """);

    // If your main repo's search joins tags, you can add JOINs here as well.
    // For minimal blast radius, we keep it simple and consistent with your legacy behaviour.

    sb.append("""
        WHERE
          (e.`date` > ? OR (e.`date` = ? AND e.start_time >= ?))
          AND e.status = 'ACTIVE'
        """);

    final boolean hasQ = (q != null && !q.isBlank());
    if (hasQ) {
        sb.append("""
            AND LOWER(CONCAT(
                  COALESCE(e.title,''),' ',
                  COALESCE(e.description,''),' ',
                  COALESCE(e.location,'')
            )) LIKE ?
            """);
    }

    // Ordering: upcoming first by date/time/title (ASC)
    sb.append(" ORDER BY e.`date` ASC, e.start_time ASC, e.title ASC ");

    final boolean doPage = (limit != null && offset != null && limit > 0 && offset >= 0);
    if (doPage) {
        sb.append(" LIMIT ? OFFSET ? ");
    }

    final java.util.List<Long> ids = new java.util.ArrayList<>();

    // Run the ID query with try-with-resources (manual JDBC)
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sb.toString())) {

        int i = 1;
        ps.setDate(i++, java.sql.Date.valueOf(useToday));
        ps.setDate(i++, java.sql.Date.valueOf(useToday));
        ps.setTimestamp(i++, java.sql.Timestamp.valueOf(useNow));

        if (hasQ) {
            final String like = "%" + q.toLowerCase().trim() + "%";
            ps.setString(i++, like);
        }
        if (doPage) {
            ps.setInt(i++, limit);
            ps.setInt(i, offset);
        }

        try (java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // event_id assumed BIGINT/INT compatible
                long id = rs.getLong(1);
                ids.add(id);
            }
        }
    } catch (java.sql.SQLException e) {
        throw new RuntimeException("searchUpcomingEnhanced query failed", e);
    }

    if (ids.isEmpty()) return java.util.List.of();

    // Load full Event rows using existing mapping the class already uses.
    // This avoids duplicating/guessing the main repo's constructor mapping.
    final java.util.List<au.edu.rmit.sept.webapp.model.Event> out = new java.util.ArrayList<>(ids.size());
    final String fetchSql = """
        SELECT event_id, organiser_id, club_id, title, description, location,
               `date`, start_time, finish_time, status, capacity
          FROM Events
         WHERE event_id = ?
        """;

    for (Long id : ids) {
        try (java.sql.Connection con = this.dataSource.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(fetchSql)) {

            ps.setLong(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Reuse the same mapping logic already present in this impl.
                    // If your impl has a private mapRow(ResultSet) method, call it here.
                    out.add(mapEventRowCompat(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            // Skip individual bad rows but keep others (or rethrow if you prefer strict)
            throw new RuntimeException("Failed to fetch Event id=" + id, e);
        }
    }

    return out;
}

/**
 * Compatibility row-mapper: adjust to the exact mapping this class already uses.
 * If you already have a private mapRow(ResultSet) in this class, you can delete
 * this method and call that instead.
 */
private au.edu.rmit.sept.webapp.model.Event mapEventRowCompat(java.sql.ResultSet rs) throws java.sql.SQLException {
    // --- If the main repo uses an Event constructor, mirror it here:
    // Example below assumes:
    // new Event(int eventId, int organiserId, int clubId, String title, String description,
    //           String location, LocalDate date, LocalDateTime startTime, String finishTime,
    //           String status, int capacity)

    final int eventId = rs.getInt("event_id");
    Long organiserId = rs.getObject("organiser_id", Long.class); // Handle nullable organiser_id
    final Long clubId = rs.getObject("organiser_id", Long.class); // Handle nullable organiser_id
    final String title = rs.getString("title");
    final String description = rs.getString("description");
    final String location = rs.getString("location");

    final java.sql.Date d = rs.getDate("date");
    final java.time.LocalDate eventDate = (d == null ? null : d.toLocalDate());

    final java.sql.Timestamp st = rs.getTimestamp("start_time");
    final java.time.LocalDateTime startDateTime = (st == null ? null : st.toLocalDateTime());

    final String finishTime = (rs.getString("finish_time"));

    final String status = rs.getString("status");
    final int capacity = rs.getInt("capacity");

    // If your Event is a record or builder, replace the constructor accordingly.
    return new au.edu.rmit.sept.webapp.model.Event(
            eventId, organiserId, clubId, title, description, location,
            eventDate, startDateTime, finishTime, status, capacity
    );


}

// ==== ADD-ONLY: CRUD for organiser flows ====

@Override
public java.util.Optional<au.edu.rmit.sept.webapp.model.Event> findById(java.lang.Long id) {
    final String sql = """
        SELECT event_id, organiser_id, club_id, title, description, location,
               `date`, start_time, finish_time, status, capacity
          FROM Events
         WHERE event_id = ?
        """;
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setLong(1, id);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return java.util.Optional.of(mapEventRowCompat(rs));
            }
            return java.util.Optional.empty();
        }
    } catch (java.sql.SQLException e) {
        throw new org.springframework.dao.DataAccessResourceFailureException("findById failed id=" + id, e);
    }
}

/** Insert event and return generated id. */
@Override
public int insertEvent(au.edu.rmit.sept.webapp.model.Event e) {
    final String sql = """
        INSERT INTO Events
          (organiser_id, club_id, title, description, location, `date`, start_time, finish_time, status, capacity)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

        // NOTE: Event record fields: int organiser_id, int club_id, LocalDate date, LocalDateTime start_time, String finish_time
        // ps.setInt(1, e.organiser_id());
        // INSERT
        if (e.organiser_id() != null) {
        ps.setInt(1, Math.toIntExact(e.organiser_id()));   // Long -> int
        } else {
        ps.setNull(1, java.sql.Types.INTEGER);
        }
        if (e.club_id() != null) {
        ps.setInt(2, Math.toIntExact(e.club_id()));   // Long -> int
        } else {
        ps.setNull(2, java.sql.Types.INTEGER);
        }
        ps.setString(3, e.title());
        ps.setString(4, e.description());
        ps.setString(5, e.location());
        // date
        if (e.date() != null) ps.setDate(6, java.sql.Date.valueOf(e.date())); else ps.setNull(6, java.sql.Types.DATE);
        // start_time stored as TIME (main code reads rs.getTime(...).toLocalTime())
        // start_time is DATETIME in DB → bind as Timestamp
        if (e.start_time() != null) ps.setTimestamp(7, Timestamp.valueOf(e.start_time())); else ps.setNull(7, java.sql.Types.TIMESTAMP);

        // finish_time stored as TIME or VARCHAR -> write as TIME if parsable, else as String
        // finish_time is DATETIME in DB -> combine date + "HH:mm" and bind as Timestamp

        setFinishDateTime(ps, 8, e.date(), e.finish_time());
        ps.setString(9, e.status());
        ps.setInt(10, e.capacity());

        ps.executeUpdate();
        try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                // event_id is INT in the model
                return keys.getInt(1);
            }
            throw new org.springframework.dao.DataAccessResourceFailureException("insertEvent: no generated key");
        }
    } catch (java.sql.SQLException ex) {
        throw new org.springframework.dao.DataAccessResourceFailureException("insertEvent failed", ex);
    }
}

/** Update by event_id (in record). Returns rows affected. */
@Override
public int updateEvent(au.edu.rmit.sept.webapp.model.Event e) {
    final String sql = """
        UPDATE Events
           SET organiser_id = ?,
               club_id      = ?,
               title        = ?,
               description  = ?,
               location     = ?,
               `date`       = ?,
               start_time   = ?,
               finish_time  = ?,
               status       = ?,
               capacity     = ?
         WHERE event_id    = ?
        """;
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        // ps.setInt(1, e.organiser_id());
        // INSERT
        if (e.organiser_id() != null) {
        ps.setInt(1, Math.toIntExact(e.organiser_id()));   // Long -> int
        } else {
        ps.setNull(1, java.sql.Types.INTEGER);
        }
        if (e.club_id() != null) {
        ps.setInt(2, Math.toIntExact(e.club_id()));   // Long -> int
        } else {
        ps.setNull(2, java.sql.Types.INTEGER);
        }
        ps.setString(3, e.title());
        ps.setString(4, e.description());
        ps.setString(5, e.location());
        if (e.date() != null) ps.setDate(6, java.sql.Date.valueOf(e.date())); else ps.setNull(6, java.sql.Types.DATE);
        if (e.start_time() != null) ps.setTimestamp(7, Timestamp.valueOf(e.start_time())); else ps.setNull(7, java.sql.Types.TIMESTAMP);

        // finish_time is DATETIME in DB -> combine date + "HH:mm" and bind as Timestamp
        setFinishDateTime(ps, 8, e.date(), e.finish_time());
        ps.setString(9, e.status());
        ps.setInt(10, e.capacity());
        ps.setInt(11, e.event_id());

        return ps.executeUpdate();
    } catch (java.sql.SQLException ex) {
        throw new org.springframework.dao.DataAccessResourceFailureException("updateEvent failed id=" + e.event_id(), ex);
    }
}

/** Soft delete = status 'CANCELLED'. Returns rows affected. */
@Override
public int cancelEventById(java.lang.Long id) {
    final String sql = "UPDATE Events SET status = 'CANCELLED' WHERE event_id = ?";
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setLong(1, id);
        return ps.executeUpdate();
    } catch (java.sql.SQLException ex) {
        throw new org.springframework.dao.DataAccessResourceFailureException("cancelEventById failed id=" + id, ex);
    }
}

/** Finish-time setter that tolerates TIME or VARCHAR column types. */
private static void setFinishTimeCompat(java.sql.PreparedStatement ps, int idx, String finish) throws java.sql.SQLException {
    if (finish == null || finish.isBlank()) {
        ps.setNull(idx, java.sql.Types.TIME);
        return;
    }
    // Try TIME first
    try {
        java.time.LocalTime ft = java.time.LocalTime.parse(finish.trim(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        ps.setTime(idx, java.sql.Time.valueOf(ft));
    } catch (Exception ignore) {
        // Fallback to VARCHAR
        ps.setString(idx, finish.trim());
    }
}

private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

private void setFinishDateTime(java.sql.PreparedStatement ps, int index,
                               LocalDate date, String finishStr) throws java.sql.SQLException {
    if (date == null || finishStr == null || finishStr.isBlank()) {
        ps.setNull(index, Types.TIMESTAMP);
        return;
    }
    LocalTime t;
    try {
        // form sends HH:mm
        t = LocalTime.parse(finishStr.trim(), HH_MM);
    } catch (Exception ignored) {
        // fallback if it already includes seconds (HH:mm:ss)
        t = LocalTime.parse(finishStr.trim());
    }
    LocalDateTime ldt = date.atTime(t);
    ps.setTimestamp(index, Timestamp.valueOf(ldt));
}


@Override
public int deleteEventById(int eventId, Long adminId, String reason) {
    String updateSql = "UPDATE Events SET status = 'DELETED' WHERE event_id = ?";
    String logSql = "INSERT INTO Deleted_Events_Log (event_id, admin_id, reason) VALUES (?, ?, ?)";

    try (Connection con = dataSource.getConnection()) {
        // Mark as deleted
        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }

        // Insert log entry
        try (PreparedStatement ps = con.prepareStatement(logSql)) {
            ps.setInt(1, eventId);
            ps.setLong(2, adminId);
            ps.setString(3, reason);
            ps.executeUpdate();
        }
        return 1;
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error deleting event " + eventId, e);
    }
}

@Override
public List<Event> findDeletedEvents() {
    List<Event> events = new ArrayList<>();
    String sql = "SELECT * FROM Events WHERE status = 'DELETED' ORDER BY date DESC";

    try (Connection con = dataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            events.add(mapEventRowCompat(rs));
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error fetching deleted events", e);
    }
    return events;
}

@Override
public List<DeletedEventLog> findDeletedLogs() {
    List<DeletedEventLog> logs = new ArrayList<>();
    String sql = "SELECT * FROM Deleted_Events_Log ORDER BY deleted_at DESC";

    try (Connection con = dataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            logs.add(new DeletedEventLog(
                rs.getInt("log_id"),
                rs.getInt("event_id"),
                rs.getLong("admin_id"),
                rs.getString("reason"),
                rs.getTimestamp("deleted_at").toLocalDateTime()
            ));
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error fetching deleted logs", e);
    }
    return logs;
}

@Override
public void clearTagsForEvent(int eventId) {
    String sql = "DELETE FROM Event_Tags WHERE event_id = ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql)) {
        stm.setInt(1, eventId);
        stm.executeUpdate();
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error clearing tags for event " + eventId, e);
    }
}

@Override
public Optional<Integer> findTagByName(String tagName) {
    String sql = "SELECT tag_id FROM Tags WHERE tag_name = ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql)) {
        stm.setString(1, tagName);
        try (ResultSet rs = stm.executeQuery()) {
            if (rs.next()) {
                return Optional.of(rs.getInt("tag_id"));
            }
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error finding tag by name " + tagName, e);
    }
    return Optional.empty();
}

@Override
public int createTag(String tagName) {
    String sql = "INSERT INTO Tags (tag_name) VALUES (?)";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        stm.setString(1, tagName);
        stm.executeUpdate();
        try (ResultSet generatedKeys = stm.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating tag failed, no ID obtained.");
            }
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error creating tag " + tagName, e);
    }
}

@Override
public void linkTagToEvent(int eventId, int tagId) {
    String sql = "INSERT INTO Event_Tags (event_id, tag_id) VALUES (?, ?)";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql)) {
        stm.setInt(1, eventId);
        stm.setInt(2, tagId);
        stm.executeUpdate();
    } catch (SQLException e) {
        // Ignore duplicate key errors, which can happen if the tag is already linked
        if (!e.getSQLState().equals("23000")) {
            throw new DataAccessResourceFailureException("Error linking tag to event", e);
        }
    }
}

@Override
public List<Tags> findAllTags() {
    List<Tags> tags = new ArrayList<>();
    String sql = "SELECT * FROM Tags ORDER BY tag_name ASC";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql);
         ResultSet rs = stm.executeQuery()) {
        while (rs.next()) {
            tags.add(new Tags(rs.getInt("tag_id"), rs.getString("tag_name")));
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error finding all tags", e);
    }
    return tags;
}

@Override
public List<Tags> findTagsByEventId(int eventId) {
    List<Tags> tags = new ArrayList<>();
    String sql = "SELECT t.* FROM Tags t JOIN Event_Tags et ON t.tag_id = et.tag_id WHERE et.event_id = ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement stm = connection.prepareStatement(sql)) {
        stm.setInt(1, eventId);
        try (ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                tags.add(new Tags(rs.getInt("tag_id"), rs.getString("tag_name")));
            }
        }
    } catch (SQLException e) {
        throw new DataAccessResourceFailureException("Error finding tags for event " + eventId, e);
    }
    return tags;
}

@Override
public List<Event> searchUpcomingByOrganiser(Long organiserId, String query, Integer page, Integer size) {
    final java.time.LocalDate today = java.time.LocalDate.now();
    final java.time.LocalDateTime now = java.time.LocalDateTime.now();

    // Build WHERE clause with organiser filter
    final StringBuilder sb = new StringBuilder();
    sb.append("""
        SELECT e.event_id
          FROM Events e
        WHERE e.organiser_id = ?
          AND (e.`date` > ? OR (e.`date` = ? AND e.start_time >= ?))
          AND e.status = 'ACTIVE'
        """);

    final boolean hasQuery = (query != null && !query.isBlank());
    if (hasQuery) {
        sb.append("""
            AND LOWER(CONCAT(
                  COALESCE(e.title,''),' ',
                  COALESCE(e.description,''),' ',
                  COALESCE(e.location,'')
            )) LIKE ?
            """);
    }

    // Ordering: upcoming first by date/time/title (ASC)
    sb.append(" ORDER BY e.`date` ASC, e.start_time ASC, e.title ASC ");

    final boolean doPage = (page != null && size != null && page >= 0 && size > 0);
    if (doPage) {
        sb.append(" LIMIT ? OFFSET ? ");
    }

    final java.util.List<Long> ids = new java.util.ArrayList<>();

    // Run the ID query
    try (java.sql.Connection con = this.dataSource.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sb.toString())) {

        int i = 1;
        ps.setLong(i++, organiserId);
        ps.setDate(i++, java.sql.Date.valueOf(today));
        ps.setDate(i++, java.sql.Date.valueOf(today));
        ps.setTimestamp(i++, java.sql.Timestamp.valueOf(now));

        if (hasQuery) {
            final String like = "%" + query.toLowerCase().trim() + "%";
            ps.setString(i++, like);
        }
        if (doPage) {
            ps.setInt(i++, size);
            ps.setInt(i, page * size);
        }

        try (java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                ids.add(id);
            }
        }
    } catch (java.sql.SQLException e) {
        throw new RuntimeException("searchUpcomingByOrganiser query failed", e);
    }

    if (ids.isEmpty()) return java.util.List.of();

    // Load full Event rows
    final java.util.List<au.edu.rmit.sept.webapp.model.Event> out = new java.util.ArrayList<>(ids.size());
    final String fetchSql = """
        SELECT event_id, organiser_id, club_id, title, description, location,
               `date`, start_time, finish_time, status, capacity
          FROM Events
         WHERE event_id = ?
        """;

    for (Long id : ids) {
        try (java.sql.Connection con = this.dataSource.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(fetchSql)) {

            ps.setLong(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long eventId = rs.getLong("event_id");
                    Long orgId = rs.getObject("organiser_id", Long.class);
                    Long clubId = rs.getObject("club_id", Long.class);
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    String location = rs.getString("location");
                    LocalDate date = rs.getDate("date").toLocalDate();
                    java.sql.Timestamp startTs = rs.getTimestamp("start_time");
                    java.sql.Timestamp finishTs = rs.getTimestamp("finish_time");
                    String status = rs.getString("status");
                    int capacity = rs.getInt("capacity");

                    LocalDateTime startTime = (startTs != null) ? startTs.toLocalDateTime() : null;
                    String finishTime = (finishTs != null) ? finishTs.toLocalDateTime().toLocalTime().toString() : null;

                    Event event = new Event(
                        (int) eventId, orgId, clubId, title, description, location,
                        date, startTime, finishTime, status, capacity
                    );
                    out.add(event);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error loading event " + id, e);
        }
    }

    return out;
}

}