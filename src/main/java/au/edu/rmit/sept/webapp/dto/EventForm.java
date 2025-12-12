package au.edu.rmit.sept.webapp.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Form backing object for creating or editing an Event.
 * Uses Bean Validation annotations for server-side validation.
 */
public class EventForm {


    private Long organiserId;
    private Long clubId;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255)
    private String location;

    @NotNull(message = "Event date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "HH:mm") // no seconds
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "HH:mm") // no seconds
    private LocalTime endTime;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|CANCELLED", message = "Status must be ACTIVE or CANCELLED")
    private String status = "ACTIVE";

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity = 1;

    private String tags;

    @AssertTrue(message = "Start time must be in 15-minute increments")
    public boolean isStartOnQuarterHour() {
        return startTime == null || startTime.getMinute() % 15 == 0;
    }

    @AssertTrue(message = "End time must be in 15-minute increments")
    public boolean isEndOnQuarterHour() {
        return endTime == null || endTime.getMinute() % 15 == 0;
    }

    @AssertTrue(message = "End time must be after start time")
    public boolean isEndAfterStart() {
        if (eventDate == null || startTime == null || endTime == null) return true; 
        LocalDateTime start = eventDate.atTime(startTime);
        LocalDateTime end   = eventDate.atTime(endTime);
        return end.isAfter(start);
    }

    @AssertTrue(message = "End time must be at least 30 minutes after start time")
    public boolean isEndAfterStartBy30() {
        if (startTime == null || endTime == null) return true; // @NotNull handles these
        return java.time.Duration.between(startTime, endTime).toMinutes() >= 30;
    }

   // Getters and Setters

    public Long getOrganiserId() { return organiserId; }
    public void setOrganiserId(Long organiserId) { this.organiserId = organiserId; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
