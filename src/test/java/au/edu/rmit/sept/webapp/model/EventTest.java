package au.edu.rmit.sept.webapp.model;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {

    private Event createDummyEvent(LocalDate date, LocalDateTime startTime) {
        return new Event(
                1,                // event_id
                101L,              // organiser_id
                202L,              // club_id
                "Sample Title",   // title
                "Sample Desc",    // description
                "Sample Location",// location
                date,             // date
                startTime,        // start_time
                "22:00",          // finish_time
                "Scheduled",      // status
                100               // capacity
        );
    }

    @Test
    void testEventFormatToday() {
        LocalDateTime now = LocalDateTime.now();
        Event eventToday = createDummyEvent(LocalDate.now(), now.withHour(15).withMinute(30));

        assertEquals("Today at 15:30", eventToday.formattedStartDateTime());
    }

    @Test
    void testEventFormatTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Event eventTomorrow = createDummyEvent(tomorrow, startTime);

        assertEquals("Tomorrow at 10:00", eventTomorrow.formattedStartDateTime());
    }

    @Test
    void testEventFormatFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        LocalDateTime startTime = LocalDateTime.now().plusDays(5).withHour(18).withMinute(45);
        Event eventFuture = createDummyEvent(futureDate, startTime);

        String expectedDate = futureDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertEquals(expectedDate + " at 18:45", eventFuture.formattedStartDateTime());
    }
}

