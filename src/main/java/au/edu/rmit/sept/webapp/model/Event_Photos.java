package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record Event_Photos(
    int photo_id,
    int event_id,
    int organiser_id,
    String url,
    LocalDateTime uploaded_at
)
{}