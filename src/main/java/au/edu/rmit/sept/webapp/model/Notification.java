package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record Notification(
        int notification_id,
        int user_id,
        String type,           
        String title,
        String message,
        String link,           
        LocalDateTime created_at,
        LocalDateTime read_at  
) {
    public boolean isRead() {
        return read_at != null;
    }

    public boolean isUnread() {
        return read_at == null;
    }
}
