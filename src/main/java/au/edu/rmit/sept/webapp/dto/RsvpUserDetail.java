package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDateTime;

public record RsvpUserDetail(
    Long userId,
    String email,
    String name,
    String course,
    LocalDateTime rsvpDate, 
    String qrCode
) {}
