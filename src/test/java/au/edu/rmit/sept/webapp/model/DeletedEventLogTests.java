package au.edu.rmit.sept.webapp.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DeletedEventLogTests {

    @Test
    void constructorAndAccessorsReturnExpectedValues() {
        LocalDateTime now = LocalDateTime.now();

        DeletedEventLog log = new DeletedEventLog(1, 2, 3L, "Test reason", now);

        assertEquals(1, log.log_id());
        assertEquals(2, log.event_id());
        assertEquals(3L, log.admin_id());
        assertEquals("Test reason", log.reason());
        assertEquals(now, log.deleted_at());
    }

    @Test
    void recordEqualityAndHashCodeWorkAsExpected() {
        LocalDateTime t1 = LocalDateTime.of(2025, 10, 10, 12, 0);
        DeletedEventLog a = new DeletedEventLog(1, 2, 3L, "Reason", t1);
        DeletedEventLog b = new DeletedEventLog(1, 2, 3L, "Reason", t1);
        DeletedEventLog c = new DeletedEventLog(2, 2, 3L, "Different", t1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void toStringContainsFieldNamesAndValues() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        DeletedEventLog log = new DeletedEventLog(10, 20, 30L, "Deleted", now);

        String s = log.toString();
        assertTrue(s.contains("log_id=10"));
        assertTrue(s.contains("event_id=20"));
        assertTrue(s.contains("admin_id=30"));
        assertTrue(s.contains("Deleted"));
    }

    @Test
    void recordIsImmutable() {
        LocalDateTime now = LocalDateTime.now();
        DeletedEventLog log = new DeletedEventLog(1, 1, 1L, "Reason", now);

        // Records are immutable; verify no mutation occurs
        assertThrows(NoSuchMethodException.class, () -> log.getClass().getMethod("setReason", String.class));
    }
}
