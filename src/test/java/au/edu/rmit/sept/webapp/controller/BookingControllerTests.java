package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.PhotoService;
import au.edu.rmit.sept.webapp.service.UserService;

@WebMvcTest(BookingController.class)
public class BookingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;
    
    @MockBean
    private PhotoService photoService;
    
    @MockBean
    private UserService userService;

    @Test
    public void myBookings_userNotLoggedIn() throws Exception {
        mockMvc.perform(get("/my-bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void myBookings_userLoggedIn() throws Exception {
        User user = new User(1, "test@example.com", "password", LocalDateTime.now());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        Event event = new Event(1, 1L, 1L, "Test Event", "Description", "Location",
                java.time.LocalDate.now(), java.time.LocalDateTime.now(), "22:00", "upcoming", 100);
        List<Event> bookedEvents = Collections.singletonList(event);

        when(eventService.getRsvpedEvents(user.getUserId())).thenReturn(bookedEvents);

        mockMvc.perform(get("/my-bookings").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("my-bookings"))
                .andExpect(model().attribute("bookedEvents", bookedEvents));
    }

    @Test
    public void cancelBooking_userNotLoggedIn() throws Exception {
        mockMvc.perform(post("/cancel-booking").param("eventId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void cancelBooking_userLoggedIn() throws Exception {
        User user = new User(1, "test@example.com", "password", LocalDateTime.now());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(post("/cancel-booking").param("eventId", "1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookings"));

        verify(eventService).cancelRsvp(1L, user.getUserId());
    }
}