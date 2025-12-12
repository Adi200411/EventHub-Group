package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckInController.class)
public class CheckInControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserService userService;

    @Test
    void checkIn_success() throws Exception {
        //checks if it sucessfully returns a success view when check-in is successful
        Long eventId = 1L;
        Long userId = 1L;
        Event event = new Event(1, 1L, 1L, "Test Event", "Description", "Location",
                LocalDate.now(), LocalDateTime.now(), "22:00", "upcoming", 100);
        User user = new User(1, "test@example.com", "password", LocalDateTime.now());

        when(eventService.checkInUser(eventId, userId)).thenReturn(true);
        when(eventService.getEventById(eventId)).thenReturn(Optional.of(event));
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/checkin/{eventId}/{userId}", eventId, userId))
                .andExpect(status().isOk())
                .andExpect(view().name("checkin-result"))
                .andExpect(model().attribute("success", true))
                .andExpect(model().attribute("message", "Check-in successful!"))
                .andExpect(model().attribute("event", event))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void checkIn_failure() throws Exception {
        //checks if it sucessfully returns a failure view when check-in fails
        Long eventId = 2L;
        Long userId = 2L;
        Event event = new Event(2, 1L, 1L, "Another Event", "Description", "Location",
                LocalDate.now(), LocalDateTime.now(), "22:00", "upcoming", 100);
        User user = new User(2, "another@example.com", "password", LocalDateTime.now());

        when(eventService.checkInUser(eventId, userId)).thenReturn(false);
        when(eventService.getEventById(eventId)).thenReturn(Optional.of(event));
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/checkin/{eventId}/{userId}", eventId, userId))
                .andExpect(status().isOk())
                .andExpect(view().name("checkin-result"))
                .andExpect(model().attribute("success", false))
                .andExpect(model().attribute("message", "Check-in failed. Invalid QR code or user not RSVP'd."));
    }
}