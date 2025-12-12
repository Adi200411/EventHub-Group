package au.edu.rmit.sept.webapp.dto;

import au.edu.rmit.sept.webapp.model.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class EventFormMapperTests {

    private Event sampleEvent() {
        return new Event(
                1,
                10L,
                20L,
                "Title",
                "Description",
                "Venue",
                LocalDate.of(2025, 10, 10),
                LocalDateTime.of(2025, 10, 10, 9, 0),
                "11:00:00",
                "ACTIVE",
                100
        );
    }

    private EventForm baseForm() {
        EventForm f = new EventForm();
        f.setTitle("Conference");
        f.setDescription("Tech conference");
        f.setLocation("Melbourne");
        f.setEventDate(LocalDate.of(2025, 10, 15));
        f.setStartTime(LocalTime.of(10, 0));
        f.setEndTime(LocalTime.of(12, 0));
        f.setStatus("ACTIVE");
        f.setCapacity(300);
        f.setOrganiserId(99L);
        f.setClubId(88L);
        return f;
    }

    // === toNewEntity ===

    @Test
    void toNewEntityBuildsEventCorrectly() {
        EventForm form = baseForm();
        Event e = EventFormMapper.toNewEntity(form, 77L, 66L);

        assertEquals(0, e.event_id());
        assertEquals(77L, e.organiser_id());
        assertEquals(66L, e.club_id());
        assertEquals("Conference", e.title());
        assertEquals("Melbourne", e.location());
        assertEquals(LocalDate.of(2025, 10, 15), e.date());
        assertEquals(LocalDateTime.of(2025, 10, 15, 10, 0), e.start_time());
        assertEquals("12:00:00", e.finish_time());
        assertEquals("ACTIVE", e.status());
        assertEquals(300, e.capacity());
    }

    @Test
    void toNewEntityFallsBackToFormValuesWhenNullArgs() {
        EventForm form = baseForm();
        Event e = EventFormMapper.toNewEntity(form, null, null);

        assertEquals(99L, e.organiser_id());
        assertEquals(88L, e.club_id());
    }

    @Test
    void toNewEntityDefaultsStatusToActiveWhenBlank() {
        EventForm form = baseForm();
        form.setStatus("  ");
        Event e = EventFormMapper.toNewEntity(form, 1L, 1L);
        assertEquals("ACTIVE", e.status());
    }

    @Test
    void toNewEntityHandlesNullDateOrTimesGracefully() {
        EventForm form = baseForm();
        form.setEventDate(null);
        form.setStartTime(null);
        form.setEndTime(null);
        Event e = EventFormMapper.toNewEntity(form, 1L, 1L);
        assertNull(e.start_time());
        assertNull(e.finish_time());
    }

    // === applyToEntity ===

    @Test
    void applyToEntityReplacesFieldsAndKeepsIds() {
        Event existing = sampleEvent();
        EventForm form = baseForm();
        form.setDescription("Updated desc");
        Event updated = EventFormMapper.applyToEntity(form, existing);

        assertEquals(existing.event_id(), updated.event_id());
        assertEquals(form.getDescription(), updated.description());
        assertEquals(form.getEventDate(), updated.date());
        assertEquals(LocalDateTime.of(2025, 10, 15, 10, 0), updated.start_time());
        assertEquals("12:00:00", updated.finish_time());
        assertEquals("ACTIVE", updated.status());
    }

    @Test
    void applyToEntityUsesExistingIdsWhenFormNull() {
        Event existing = sampleEvent();
        EventForm form = baseForm();
        form.setOrganiserId(null);
        form.setClubId(null);

        Event updated = EventFormMapper.applyToEntity(form, existing);

        assertEquals(existing.organiser_id(), updated.organiser_id());
        assertEquals(existing.club_id(), updated.club_id());
    }

    @Test
    void applyToEntityKeepsExistingFinishTimeWhenEndTimeMissing() {
        Event existing = sampleEvent();
        EventForm form = baseForm();
        form.setEndTime(null);

        Event updated = EventFormMapper.applyToEntity(form, existing);
        assertEquals(existing.finish_time(), updated.finish_time());
    }

    @Test
    void applyToEntityTrimsAndDefaultsStatus() {
        Event existing = sampleEvent();
        EventForm form = baseForm();
        form.setStatus("   ");
        Event updated = EventFormMapper.applyToEntity(form, existing);
        assertEquals(existing.status(), updated.status());
    }

    // === fromEntity ===

    @Test
    void fromEntityConvertsAllFieldsProperly() {
        Event e = sampleEvent();
        EventForm f = EventFormMapper.fromEntity(e);

        assertEquals(e.organiser_id(), f.getOrganiserId());
        assertEquals(e.club_id(), f.getClubId());
        assertEquals(e.title(), f.getTitle());
        assertEquals(e.description(), f.getDescription());
        assertEquals(e.location(), f.getLocation());
        assertEquals(e.date(), f.getEventDate());
        assertEquals(LocalTime.of(9, 0), f.getStartTime());
        assertEquals(LocalTime.of(11, 0), f.getEndTime());
        assertEquals(e.status(), f.getStatus());
        assertEquals(e.capacity(), f.getCapacity());
    }

    @Test
    void fromEntityHandlesNullTimesGracefully() {
        Event e = new Event(
                1, 1L, 1L,
                "T", "D", "L",
                LocalDate.of(2025, 1, 1),
                null,
                null,
                "ACTIVE",
                10
        );

        EventForm f = EventFormMapper.fromEntity(e);
        assertNull(f.getStartTime());
        assertNull(f.getEndTime());
    }

    // === parseTimeSafe (indirect via fromEntity) ===

    @Test
    void fromEntityParsesVariousTimeFormats() {
        String[] validTimes = {
                "11:30",
                "11:30:00",
                "2025-10-10 11:30",
                "2025-10-10 11:30:00",
                "2025-10-10T11:30:00"
        };

        for (String t : validTimes) {
            Event e = new Event(
                    1, 1L, 1L,
                    "T", "D", "L",
                    LocalDate.of(2025, 1, 1),
                    LocalDateTime.of(2025, 1, 1, 9, 0),
                    t,
                    "ACTIVE",
                    10
            );
            EventForm f = EventFormMapper.fromEntity(e);
            assertNotNull(f.getEndTime(), "Expected parse success for " + t);
        }
    }

    @Test
    void fromEntityThrowsForInvalidFinishTime() {
        Event e = new Event(
                1, 1L, 1L,
                "T", "D", "L",
                LocalDate.of(2025, 1, 1),
                LocalDateTime.of(2025, 1, 1, 9, 0),
                "invalid-time",
                "ACTIVE",
                10
        );
        assertThrows(IllegalArgumentException.class, () -> EventFormMapper.fromEntity(e));
    }
}
