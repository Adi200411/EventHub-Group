package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record Event_Feedback(
    int feedback_id,
    int event_id,
    int user_id,
    int rating, // 1-5
    String comments,
    LocalDateTime feedback_date
){}