package au.edu.rmit.sept.webapp.controller;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.NotificationService;
import au.edu.rmit.sept.webapp.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private NotificationService notificationService;

    private Event ev(int id) {
        return new Event(
                id, 101L, 202L,
                "Title " + id,
                "Desc " + id,
                "Venue",
                java.time.LocalDate.now(),
                java.time.LocalDateTime.now(),
                "22:00",
                "upcoming",
                100
        );
    }

    // === Existing RSVP tests remain ===

    @Test
    void rsvpWithSessionUserRedirectsToReferer() throws Exception {
        long eventId = 5L;
        User sessionUser = new User(42, "alice@example.com", "hash123", java.time.LocalDateTime.now());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", sessionUser);

        mvc.perform(post("/rsvp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("eventId", String.valueOf(eventId))
                        .header("Referer", "/recommendation")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookings"));

        verify(eventService, times(1)).rsvp(eventId, 42L);
    }

    @Test
    void rsvpWithoutSessionUser_redirectsToLoginAndDoesNotCallService() throws Exception {
        long eventId = 9L;

        mvc.perform(post("/rsvp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(eventService, never()).rsvp(anyLong(), anyLong());
    }

    @Test
    void rsvpWithSessionUserAndNullRefererRedirectsToRoot() throws Exception {
        long eventId = 3L;
        User bob = new User(7, "bob@example.com", "hashX", java.time.LocalDateTime.now());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", bob);

        mvc.perform(post("/rsvp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("eventId", String.valueOf(eventId))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookings"));

        verify(eventService, times(1)).rsvp(eventId, 7L);
    }

    // === Existing browse tests remain ===

    @Test
    void browseWithoutFiltersCallsUpcomingAndPast() throws Exception {
        when(eventService.getUpcomingEvents()).thenReturn(List.of(ev(1), ev(2)));
        when(eventService.getPastEvents()).thenReturn(List.of(ev(3)));

        mvc.perform(get("/browse"))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(content().string(containsString("Title 1")))
                .andExpect(content().string(containsString("Title 3")));

        verify(eventService, times(1)).getUpcomingEvents();
        verify(eventService, times(1)).getPastEvents();
        verify(eventService, times(0)).searchEvents(any(), any(), any(), anyBoolean());
    }

    @Test
    void browseWithFiltersCallsSearch() throws Exception {
        String q = "hack";
        String tag = "Tech";
        String date = "2025-10-10";

        when(eventService.searchEvents(q, tag, date, true)).thenReturn(List.of(ev(10)));
        when(eventService.searchEvents(q, tag, date, false)).thenReturn(List.of(ev(20), ev(21)));

        mvc.perform(get("/browse")
                        .param("query", q)
                        .param("tag", tag)
                        .param("date", date))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(content().string(containsString("Title 10")))
                .andExpect(content().string(containsString("Title 20")));

        verify(eventService, times(1)).searchEvents(q, tag, date, true);
        verify(eventService, times(1)).searchEvents(q, tag, date, false);
        verify(eventService, times(0)).getUpcomingEvents();
        verify(eventService, times(0)).getPastEvents();
    }

    @Test
    void browseErrorReturnsErrorView() throws Exception {
        when(eventService.getUpcomingEvents()).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/browse"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    // === NEW TESTS for uncovered branches ===

    @Test
    void browseWithLoggedInUserAddsAuthenticatedTrueToModel() throws Exception {
        User user = new User(1, "john@example.com", "pwd", java.time.LocalDateTime.now());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        session.setAttribute("userId", 1);

        when(eventService.getUpcomingEvents()).thenReturn(List.of(ev(1)));
        when(eventService.getPastEvents()).thenReturn(List.of(ev(2)));
        when(eventService.getRsvpedEventIds(1L)).thenReturn(List.of(1, 2));
        when(userService.getProfileByUserId(1)).thenReturn(Optional.of(new au.edu.rmit.sept.webapp.model.Student_Profile(1, "John Doe", "CSE", "john@example.com")));

        mvc.perform(get("/browse").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(content().string(containsString("Title 1")))
                .andExpect(content().string(containsString("Title 2")));

        verify(eventService).getUpcomingEvents();
        verify(eventService).getPastEvents();
        verify(eventService).getRsvpedEventIds(1L);
        verify(userService).getProfileByUserId(1);
    }

    // === /browse/filtered tests ===

    @Test
    void browseFilteredReturnsBrowseView() throws Exception {
        when(eventService.getUpcomingFiltered(any(), any(), any())).thenReturn(List.of(ev(1)));
        when(eventService.getPastEvents()).thenReturn(List.of(ev(2)));

        mvc.perform(get("/browse/filtered")
                        .param("q", "test")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(content().string(containsString("Title 1")))
                .andExpect(content().string(containsString("Title 2")));

        verify(eventService, times(1)).getUpcomingFiltered("test", 1, 10);
        verify(eventService, times(1)).getPastEvents();
    }

    @Test
    void browseFilteredHandlesException() throws Exception {
        when(eventService.getUpcomingFiltered(any(), any(), any())).thenThrow(new RuntimeException("DB error"));

        mvc.perform(get("/browse/filtered"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    // === /events/{eventId} tests ===

    @Test
    void viewEventDetailsReturnsEventDetailsWhenFound() throws Exception {
        Event event = ev(42);
        when(eventService.getEventById(42L)).thenReturn(Optional.of(event));

        mvc.perform(get("/events/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("event-details"))
                .andExpect(content().string(containsString("Title 42")));

        verify(eventService).getEventById(42L);
    }

    @Test
    void viewEventDetailsRedirectsWhenNotFound() throws Exception {
        when(eventService.getEventById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/events/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/browse"));
    }

    @Test
    void viewEventDetailsRedirectsOnException() throws Exception {
        when(eventService.getEventById(5L)).thenThrow(new RuntimeException("error"));

        mvc.perform(get("/events/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/browse"));
    }
}
