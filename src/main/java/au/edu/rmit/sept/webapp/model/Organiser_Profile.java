package au.edu.rmit.sept.webapp.model;

public record Organiser_Profile(
        int organiser_id,
        int user_id,
        int club_id,
        String role_title
    )
{}