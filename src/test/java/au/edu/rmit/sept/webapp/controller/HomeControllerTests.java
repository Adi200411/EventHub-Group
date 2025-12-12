package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.ClubService;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;

@WebMvcTest(HomeController.class)
public class HomeControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserService userService;

    @MockBean
    private ClubService clubService;

    @MockBean
    private UserRepository userRepository;

    private User createTestUser() {
        return new User(1, "test@example.com", "password", LocalDateTime.now());
    }

    private Event createTestEvent(int id) {
        return new Event(id, 1L, 1L, "Test Event " + id, "Description", "Location",
                java.time.LocalDate.now(), java.time.LocalDateTime.now(), "22:00", "upcoming", 100);
    }

    // @Test
    // void login_should_returnAccountSetup_when_credentialsAreValid() throws Exception {
    //     User user = createTestUser();
    //     when(userService.findByUsernameAndPassword("test@example.com", "password")).thenReturn(user);

    //     mvc.perform(post("/user/login")
    //                     .param("email", "test@example.com")
    //                     .param("password", "password"))
    //             .andExpect(status().isOk())
    //             .andExpect(view().name("account-setup"))
    //             .andExpect(model().attribute("user", user));
    // }

    // @Test
    // void login_should_returnLogin_when_credentialsAreInvalid() throws Exception {
    //     when(userService.findByUsernameAndPassword("test@example.com", "wrongpassword")).thenReturn(null);

    //     mvc.perform(post("/user/login")
    //                     .param("email", "test@example.com")
    //                     .param("password", "wrongpassword"))
    //             .andExpect(status().isOk())
    //             .andExpect(view().name("login"))
    //             .andExpect(model().attribute("error", "Invalid credentials"));
    // }

    @Test
    void register_should_returnAccountSetup_when_registrationIsSuccessful() throws Exception {
        User user = createTestUser();
        when(userService.register("test@example.com", "password")).thenReturn(user);

        mvc.perform(post("/sign-up")
                        .param("email", "test@example.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/account-setup"));
    }

    @Test
    void register_should_returnSignup_when_userAlreadyExists() throws Exception {
        when(userService.register("test@example.com", "password")).thenReturn(null);

        mvc.perform(post("/sign-up")
                        .param("email", "test@example.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:signup"));
    }


    @Test
    void landingPage_should_loadEvents() throws Exception {
        List<Event> events = List.of(createTestEvent(1), createTestEvent(2));
        when(eventService.getUpcomingEvents()).thenReturn(events);

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(model().attribute("events", hasSize(2)));
    }

    @Test
    void landingPage_should_handleNoUpcomingEvents() throws Exception {
        when(eventService.getUpcomingEvents()).thenReturn(Collections.emptyList());
        when(eventService.getAllEvents()).thenReturn(List.of(createTestEvent(3)));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(model().attribute("events", hasSize(1)));
    }

    @Test
    void landingPage_should_handleServiceError() throws Exception {
        when(eventService.getUpcomingEvents()).thenThrow(new RuntimeException("Database error"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(model().attribute("error", "Unable to load events at this time"));
    }

    @Test
    void recommended_should_getRecommendationsForUser() throws Exception {
        User user = new User(42, "test@example.com", "password", LocalDateTime.now());
        List<Event> recommendedEvents = List.of(createTestEvent(10), createTestEvent(11));

        when(eventService.getRecommendedEvents(42L)).thenReturn(recommendedEvents);
        when(eventService.getRsvpedEventIds(42L)).thenReturn(Collections.emptyList());
        when(userRepository.isOrganiserForEvent(42, 10)).thenReturn(false);
        when(userRepository.isOrganiserForEvent(42, 11)).thenReturn(false);

        mvc.perform(get("/events/recommended")
                .sessionAttr("user", user)
                .sessionAttr("userId", user.user_id()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation"))
                .andExpect(model().attribute("events", hasSize(2)));
    }

    @Test
    void recommended_should_useFallbackUser_when_notLoggedIn() throws Exception {
        List<Event> recommendedEvents = List.of(createTestEvent(10));
        when(eventService.getRecommendedEvents(1L)).thenReturn(recommendedEvents);
        when(eventService.getRsvpedEventIds(1L)).thenReturn(Collections.emptyList());

        mvc.perform(get("/recommendation"))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation"))
                .andExpect(model().attribute("events", hasSize(1)));
    }

    @Test
    void signupPage_should_loadSuccessfully() throws Exception {
        mvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void loginPage_should_loadSuccessfully() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void accountSetupPage_should_loadSuccessfully() throws Exception {
        when(clubService.getAllClubs()).thenReturn(Collections.emptyList());
        
        mvc.perform(get("/account-setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-setup"));
    }
}