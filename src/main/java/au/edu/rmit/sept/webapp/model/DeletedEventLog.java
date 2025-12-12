package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record DeletedEventLog(
        int log_id,
        int event_id,
        Long admin_id,
        String reason,
        LocalDateTime deleted_at
) {}
