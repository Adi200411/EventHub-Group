package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record RSVP(
        int event_id,
        int user_id,
        LocalDateTime rsvp_time,
        String qr_code

    )
{}
