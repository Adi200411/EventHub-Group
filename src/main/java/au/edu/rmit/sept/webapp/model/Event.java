package au.edu.rmit.sept.webapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Event(
        int event_id,
        Long organiser_id,
        Long club_id,
        String title,
        String description,
        String location,
        LocalDate date,
        LocalDateTime start_time,
        String finish_time,
        String status,
        int capacity
    )   {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String formattedStartDateTime() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        String time = start_time.format(TIME_FORMATTER);

        if (date.isEqual(today)) {
            return "Today at " + time;
        } else if (date.isEqual(tomorrow)) {
            return "Tomorrow at " + time;
        } else {
            return date.format(DATE_FORMATTER) + " at " + time;
        }
    }
}