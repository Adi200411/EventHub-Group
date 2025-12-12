package au.edu.rmit.sept.webapp.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventFormTests {

    private Validator validator;
    private EventForm form;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        form = new EventForm();
        form.setTitle("Hackathon 2025");
        form.setDescription("24-hour innovation challenge");
        form.setLocation("Melbourne");
        form.setEventDate(LocalDate.of(2025, 10, 10));
        form.setStartTime(LocalTime.of(9, 0));
        form.setEndTime(LocalTime.of(11, 0));
        form.setStatus("ACTIVE");
        form.setCapacity(100);
    }

    // === Bean validation: positive case ===

    @Test
    void validEventFormShouldPassValidation() {
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty(), "Expected no validation errors for valid form");
    }

    // === Field-level @NotBlank / @NotNull ===

    @Test
    void titleIsRequired() {
        form.setTitle("");
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title is required")));
    }

    @Test
    void locationIsRequired() {
        form.setLocation(null);
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Location is required")));
    }

    @Test
    void eventDateIsRequired() {
        form.setEventDate(null);
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Event date is required")));
    }

    @Test
    void startTimeIsRequired() {
        form.setStartTime(null);
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Start time is required")));
    }

    @Test
    void endTimeIsRequired() {
        form.setEndTime(null);
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End time is required")));
    }

    @Test
    void statusMustBeActiveOrCancelled() {
        form.setStatus("DELAYED");
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Status must be ACTIVE or CANCELLED")));
    }

    @Test
    void capacityMustBeAtLeastOne() {
        form.setCapacity(0);
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Capacity must be at least 1")));
    }

    // === Custom @AssertTrue methods ===

    @Test
    void startTimeMustBeOnQuarterHour() {
        form.setStartTime(LocalTime.of(10, 7)); // invalid
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Start time must be in 15-minute increments")));
    }

    @Test
    void endTimeMustBeOnQuarterHour() {
        form.setEndTime(LocalTime.of(11, 23)); // invalid
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End time must be in 15-minute increments")));
    }

    @Test
    void endTimeMustBeAfterStartTime() {
        form.setStartTime(LocalTime.of(14, 0));
        form.setEndTime(LocalTime.of(13, 0)); // before start
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End time must be after start time")));
    }

    @Test
    void endTimeMustBeAtLeast30MinutesAfterStartTime() {
        form.setStartTime(LocalTime.of(10, 0));
        form.setEndTime(LocalTime.of(10, 15)); // only 15 mins apart
        Set<ConstraintViolation<EventForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End time must be at least 30 minutes after start time")));
    }

    // === Getters & Setters ===

    @Test
    void testGettersAndSetters() {
        EventForm f = new EventForm();
        f.setOrganiserId(10L);
        f.setClubId(20L);
        f.setTitle("Test Event");
        f.setDescription("Description");
        f.setLocation("Location");
        f.setEventDate(LocalDate.of(2025, 1, 1));
        f.setStartTime(LocalTime.of(9, 0));
        f.setEndTime(LocalTime.of(10, 0));
        f.setStatus("ACTIVE");
        f.setCapacity(50);

        assertEquals(10L, f.getOrganiserId());
        assertEquals(20L, f.getClubId());
        assertEquals("Test Event", f.getTitle());
        assertEquals("Description", f.getDescription());
        assertEquals("Location", f.getLocation());
        assertEquals(LocalDate.of(2025, 1, 1), f.getEventDate());
        assertEquals(LocalTime.of(9, 0), f.getStartTime());
        assertEquals(LocalTime.of(10, 0), f.getEndTime());
        assertEquals("ACTIVE", f.getStatus());
        assertEquals(50, f.getCapacity());
    }

    // === Edge cases ===

    @Test
    void nullTimesDoNotBreakAssertTrueMethods() {
        EventForm f = new EventForm();
        f.setStartTime(null);
        f.setEndTime(null);
        f.setEventDate(null);

        // Should not throw NPE
        assertDoesNotThrow(f::isStartOnQuarterHour);
        assertDoesNotThrow(f::isEndOnQuarterHour);
        assertDoesNotThrow(f::isEndAfterStart);
        assertDoesNotThrow(f::isEndAfterStartBy30);
    }
}
