package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

import au.edu.rmit.sept.webapp.model.DeletedEventLog;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.AdminService;
import au.edu.rmit.sept.webapp.service.UserService;


@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserService userService;

    // helper for event
    private Event ev(int id) {
        return new Event(
                id, 10L, 20L,
                "Title " + id,
                "Desc " + id,
                "Venue",
                LocalDate.now(),
                LocalDateTime.now(),
                "22:00",
                "upcoming",
                100
        );
    }

    // helper for creating admin session with user
    private MockHttpSession createAdminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "ADMIN");
        User adminUser = new User(1, "admin@test.com", "hashedpass", LocalDateTime.now(), "ACTIVE");
        session.setAttribute("user", adminUser);
        session.setAttribute("userId", 1);
        
        // Mock the user service call
        Student_Profile profile = new Student_Profile(1, "Admin User", "Computer Science", "Technology");
        when(userService.getProfileByUserId(1)).thenReturn(Optional.of(profile));
        
        return session;
    }

    // --- Access control ---

    @Test
    void listAllEventsRedirectsIfNotAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "USER");

        mvc.perform(get("/admin/events").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        verify(adminService, never()).getAllEvents();
    }

    // --- listAllEvents() ---

    @Test
    void listAllEventsReturnsAdminEventsView() throws Exception {
        MockHttpSession session = createAdminSession();

        when(adminService.getAllEvents()).thenReturn(List.of(ev(1), ev(2)));

        mvc.perform(get("/admin/events").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/events"))
                .andExpect(content().string(containsString("Title 1")));

        verify(adminService, times(1)).getAllEvents();
    }

    // --- viewEvent() ---

    @Test
    void viewEventRedirectsIfNotAdmin() throws Exception {
        mvc.perform(get("/admin/events/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void viewEventShowsEventDetailsIfFound() throws Exception {
        MockHttpSession session = createAdminSession();
        Event event = ev(5);
        when(adminService.getEventById(5L)).thenReturn(Optional.of(event));

        mvc.perform(get("/admin/events/5").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/event_detail"))
                .andExpect(content().string(containsString("Title 5")));

        verify(adminService, times(1)).getEventById(5L);
    }

    @Test
    void viewEventShowsErrorIfNotFound() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminService.getEventById(9L)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/events/9").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Event not found")));
    }

    // --- editEventForm() ---

    @Test
    void editEventFormShowsFormIfEventFound() throws Exception {
        MockHttpSession session = createAdminSession();
        Event e = ev(7);
        when(adminService.getEventById(7L)).thenReturn(Optional.of(e));

        mvc.perform(get("/admin/events/7/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/Admin_event_form"))
                .andExpect(content().string(containsString("Title 7")));
    }

    @Test
    void editEventFormShowsErrorIfEventMissing() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminService.getEventById(8L)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/events/8/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Event not found")));
    }

    // --- saveEventEdit() ---

    @Test
    void saveEventEditRedirectsIfNotAdmin() throws Exception {
        mvc.perform(post("/admin/events/3/edit")
                        .param("title", "t")
                        .param("description", "d")
                        .param("location", "l")
                        .param("status", "s")
                        .param("capacity", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        verify(adminService, never()).updateEvent(any());
    }

    @Test
    void saveEventEditUpdatesAndRedirects() throws Exception {
        MockHttpSession session = createAdminSession();

        Event e = ev(5);
        when(adminService.getEventById(5L)).thenReturn(Optional.of(e));

        mvc.perform(post("/admin/events/5/edit")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "NewTitle")
                        .param("description", "NewDesc")
                        .param("location", "NewLoc")
                        .param("status", "updated")
                        .param("capacity", "50")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/events"));

        verify(adminService, times(1)).updateEvent(any(Event.class));
    }

    // --- deleteEvent() ---

    @Test
    void deleteEventRedirectsIfNotAdmin() throws Exception {
        mvc.perform(post("/admin/events/10/delete")
                        .param("reason", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        verify(adminService, never()).deleteEventById(anyLong(), anyLong(), any());
    }

    @Test
    void deleteEventFailsReturnsErrorView() throws Exception {
        MockHttpSession session = createAdminSession();
        session.setAttribute("userId", 5L);

        when(adminService.deleteEventById(eq(7L), eq(5L), eq("test"))).thenReturn(0);

        mvc.perform(post("/admin/events/7/delete")
                        .param("reason", "test")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Failed to delete event")));
    }

    @Test
    void deleteEventSuccessRedirectsToDeleted() throws Exception {
        MockHttpSession session = createAdminSession();
        session.setAttribute("userId", 3L);

        when(adminService.deleteEventById(2L, 3L, "reason")).thenReturn(1);

        mvc.perform(post("/admin/events/2/delete")
                        .param("reason", "reason")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/events/deleted"));

        verify(adminService, times(1)).deleteEventById(2L, 3L, "reason");
    }

    // --- viewDeletedEvents() ---

    @Test
    void viewDeletedEventsRedirectsIfNotAdmin() throws Exception {
        mvc.perform(get("/admin/events/deleted"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void viewDeletedEventsShowsDeletedList() throws Exception {
        MockHttpSession session = createAdminSession();

        when(adminService.getDeletedEvents()).thenReturn(List.of(ev(1)));
        when(adminService.getDeletedEventLogs()).thenReturn(List.of(
                new DeletedEventLog(1, 1, 1L, "reason", LocalDateTime.now())
        ));

        mvc.perform(get("/admin/events/deleted").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/deleted_events"))
                .andExpect(content().string(containsString("Title 1")))
                .andExpect(content().string(containsString("reason")));

        verify(adminService, times(1)).getDeletedEvents();
        verify(adminService, times(1)).getDeletedEventLogs();
    }
}
