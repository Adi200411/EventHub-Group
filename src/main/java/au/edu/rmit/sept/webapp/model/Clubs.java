package au.edu.rmit.sept.webapp.model;

import java.sql.Time;
import java.time.LocalDate;

public record Clubs(
        int club_id,
        int created_by,
        String name,
        String description,
        LocalDate created_at
    )
{}