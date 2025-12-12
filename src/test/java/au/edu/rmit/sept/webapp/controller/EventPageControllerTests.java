package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import au.edu.rmit.sept.webapp.dto.EventForm;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;

@WebMvcTest(EventPageController.class)
class EventPageControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;
    
    @MockBean
    private UserService userService;  

    private MockHttpSession adminSession;
    private MockHttpSession organiserSession;
    private MockHttpSession userSession;

    @BeforeEach
    void setup() {
        adminSession = new MockHttpSession();
        adminSession.setAttribute("role", "ADMIN");
        adminSession.setAttribute("organiserId", 1L);
        adminSession.setAttribute("userId", 1);

        organiserSession = new MockHttpSession();
        organiserSession.setAttribute("role", "ORGANISER");
        organiserSession.setAttribute("organiserId", 5L);
        organiserSession.setAttribute("clubId", 2L);
        organiserSession.setAttribute("userId", 5);

        userSession = new MockHttpSession();
        userSession.setAttribute("role", "USER");
        
        // Mock organiser profile lookup for the create test
        Organiser_Profile organiserProfile = new Organiser_Profile(5, 5, 2, "Organiser");
        when(userService.findOrganiserById(5)).thenReturn(Optional.of(organiserProfile));
    }

    // ---------- LIST ----------
    @Test
    void list_shouldReturnEventsForOrganiser() throws Exception {
        when(eventService.getUpcomingFilteredByOrganiser(eq(5L), any(), anyInt(), anyInt())).thenReturn(List.of(
                new Event(1, 5L, 2L, "T", "D", "L",
                        LocalDate.now(), LocalDateTime.now(), "17:00", "ACTIVE", 50)
        ));

        mockMvc.perform(get("/organiser/events").session(organiserSession))
                .andExpect(status().isOk())
                .andExpect(view().name("organiser/events_list"));
    }

    @Test
    void list_shouldReturnAllEventsForAdmin() throws Exception {
        when(eventService.getUpcomingFiltered(any(), anyInt(), anyInt())).thenReturn(List.of(
                new Event(1, 5L, 2L, "T", "D", "L",
                        LocalDate.now(), LocalDateTime.now(), "17:00", "ACTIVE", 50),
                new Event(2, 3L, 1L, "T2", "D2", "L2",
                        LocalDate.now(), LocalDateTime.now(), "18:00", "ACTIVE", 30)
        ));

        mockMvc.perform(get("/organiser/events").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("organiser/events_list"));
    }

    @Test
    void list_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(get("/organiser/events").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void list_shouldReturnErrorViewOnException() throws Exception {
        when(eventService.getUpcomingFilteredByOrganiser(eq(5L), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("DB fail"));

        mockMvc.perform(get("/organiser/events").session(organiserSession))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    // ---------- NEW FORM ----------
    @Test
    void newForm_shouldReturnFormForAdmin() throws Exception {
        mockMvc.perform(get("/organiser/events/new").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("organiser/event_form"));
    }

    @Test
    void newForm_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(get("/organiser/events/new").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

        //   CREATE 
        @Test
        void create_shouldRedirectAfterSuccess() throws Exception {
        // ✅ Admin context skips userService call
        adminSession.setAttribute("role", "ADMIN");

        when(eventService.createEvent(any(Event.class))).thenReturn(
                new Event(1, 1L, null, "Sample", "desc", "loc",
                        LocalDate.now(), LocalDateTime.now(), "18:00", "ACTIVE", 100));

        mockMvc.perform(post("/organiser/events")
                        .session(adminSession)
                        .param("title", "Test")
                        .param("description", "D")
                        .param("location", "L")
                        .param("capacity", "50")
                        .param("eventDate", LocalDate.now().toString())
                        .param("startTime", "10:00")
                        .param("endTime", "12:00")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events?created=1"));

        verify(eventService, times(1)).createEvent(any(Event.class));
        }

    @Test
    void create_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(post("/organiser/events").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void create_shouldRejectIfMissingFields() throws Exception {
        mockMvc.perform(post("/organiser/events")
                        .session(organiserSession)
                        .param("title", "") // Missing title
                        .param("description", "D")
                        .param("location", "") // Missing location
                        .param("capacity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events/new"));
    }

    @Test
        void create_shouldRejectIfAllFieldsMissing() throws Exception {
        mockMvc.perform(post("/organiser/events")
                        .session(organiserSession)) // no params at all
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events/new"));

        verify(eventService, never()).createEvent(any());
        }


    // ---------- EDIT FORM ----------
    @Test
    void editForm_shouldReturnFormForAdmin() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(Optional.of(
                new Event(1, 5L, 2L, "Title", "Desc", "Loc",
                        LocalDate.now(), LocalDateTime.now(), "17:00", "ACTIVE", 10)));

        mockMvc.perform(get("/organiser/events/1/edit").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("organiser/event_form"));
    }


    @Test
    void editForm_shouldThrowIfEventNotFound() throws Exception {
        when(eventService.getEventById(anyLong())).thenReturn(Optional.empty());

        // Expect a ServletException caused by RuntimeException("Event not found")
        jakarta.servlet.ServletException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            jakarta.servlet.ServletException.class,
           () -> mockMvc.perform(get("/organiser/events/9/edit").session(organiserSession)).andReturn()
        );

        // Optionally verify the root cause for clarity
        org.junit.jupiter.api.Assertions.assertTrue(
            thrown.getCause().getMessage().contains("Event not found"),
            "Expected cause message to contain 'Event not found'"
        );
    }


    @Test
    void editForm_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(get("/organiser/events/1/edit").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    // ---------- UPDATE ----------
    @Test
    void update_shouldRedirectAfterSuccess() throws Exception {
        when(eventService.getEventById(anyLong())).thenReturn(Optional.of(
                new Event(1, 5L, 2L, "Old", "Old", "Old",
                        LocalDate.now(), LocalDateTime.now(), "17:00", "ACTIVE", 10)));

        mockMvc.perform(post("/organiser/events/1")
                        .session(organiserSession)
                        .param("title", "Updated")
                        .param("description", "New D")
                        .param("location", "L")
                        .param("capacity", "50")
                        .param("eventDate", LocalDate.now().toString())
                        .param("startTime", "10:00")
                        .param("endTime", "12:00")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events?updated=1"));

        verify(eventService).updateEvent(eq(1L), any(Event.class));
    }

    @Test
    void update_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(post("/organiser/events/1").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    // ---------- CANCEL ----------
    @Test
    void cancel_shouldRedirectAfterSuccess() throws Exception {
        mockMvc.perform(post("/organiser/events/1/cancel").session(organiserSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events?cancelled=1"));
        verify(eventService).deleteEvent(1L);
    }

    @Test
    void cancel_shouldRedirectIfNotAuthorized() throws Exception {
        mockMvc.perform(post("/organiser/events/1/cancel").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    // ---------- VALIDATE TIME ----------
    @Test
    void create_shouldRejectIfEndBeforeStart() throws Exception {
        // Form where end < start
        EventForm badForm = new EventForm();
        badForm.setStartTime(LocalTime.of(18, 0));
        badForm.setEndTime(LocalTime.of(17, 0));

        // BindingResult is simulated by invalid params
        mockMvc.perform(post("/organiser/events")
                        .session(organiserSession)
                        .param("title", "X")
                        .param("description", "Y")
                        .param("location", "Z")
                        .param("capacity", "5")
                        .param("startTime", "18:00")
                        .param("endTime", "17:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organiser/events/new"));
    }
}
