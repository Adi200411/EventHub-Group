package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record Attendance(
        int event_id,
        int user_id,
        LocalDateTime checkin_time
    )
{}
