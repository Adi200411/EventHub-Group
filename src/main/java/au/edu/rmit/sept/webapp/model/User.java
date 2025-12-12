package au.edu.rmit.sept.webapp.model;

import java.time.LocalDateTime;

public record User(
        int user_id,
        String email,
        String password_hash,
        LocalDateTime created_at,
        String status
    ) {
    public Long getUserId() {
        return (long) user_id;
    }
    //  Backward-compatible constructor for older tests
    public User(int user_id, String email, String password_hash, LocalDateTime created_at) {
        this(user_id, email, password_hash, created_at, "ACTIVE");
    }
}

