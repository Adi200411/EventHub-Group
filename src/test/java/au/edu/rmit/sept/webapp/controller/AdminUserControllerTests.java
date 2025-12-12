package au.edu.rmit.sept.webapp.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.AdminUserService;
import au.edu.rmit.sept.webapp.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private UserService userService;

    // Helper: create a sample user
    private User user(long id, String email, String role) {
        return new User((int) id, email, "hash123", LocalDateTime.now());
    }

    // Helper: create an admin session with proper user and role attributes
    private MockHttpSession createAdminSession() {
        MockHttpSession session = new MockHttpSession();
        User adminUser = user(1, "admin@example.com", "ADMIN");
        session.setAttribute("user", adminUser);
        session.setAttribute("role", "ADMIN");
        return session;
    }

    // === Access control ===

    @Test
    void listAllUsersRedirectsIfNotAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "USER");

        mvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        verify(adminUserService, never()).getAllUsers();
    }

    // === listAllUsers() ===

    @Test
    void listAllUsersDisplaysUsersForAdmin() throws Exception {
        MockHttpSession session = createAdminSession();

        when(adminUserService.getAllUsers()).thenReturn(List.of(user(1, "a@x.com", "USER")));

        mvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("a@x.com")));

        verify(adminUserService, times(1)).getAllUsers();
    }

    // === viewUser() ===

    @Test
    void viewUserRedirectsIfNotAdmin() throws Exception {
        mvc.perform(get("/admin/users/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void viewUserShowsDetailsIfFound() throws Exception {
        MockHttpSession session = createAdminSession();
        User u = user(5, "bob@example.com", "USER");

        when(adminUserService.getUserById(5L)).thenReturn(Optional.of(u));

        mvc.perform(get("/admin/users/5").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_detail"))
                .andExpect(content().string(containsString("bob@example.com")));

        verify(adminUserService, times(1)).getUserById(5L);
    }

    @Test
    void viewUserShowsErrorIfNotFound() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.getUserById(9L)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/users/9").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("User not found")));

        verify(adminUserService, times(1)).getUserById(9L);
    }

    // === deactivateUser() ===

    @Test
    void deactivateUserRedirectsIfNotAdmin() throws Exception {
        mvc.perform(post("/admin/users/10/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        verify(adminUserService, never()).deactivateUser(any());
    }

    @Test
    void deactivateUserReturnsErrorIfFails() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.deactivateUser(3L)).thenReturn(false);

        mvc.perform(post("/admin/users/3/deactivate").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Failed to deactivate user")));
    }

    @Test
    void deactivateUserRedirectsOnSuccess() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.deactivateUser(2L)).thenReturn(true);

        mvc.perform(post("/admin/users/2/deactivate").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminUserService, times(1)).deactivateUser(2L);
    }

    // === reactivateUser() ===

    @Test
    void reactivateUserRedirectsIfNotAdmin() throws Exception {
        mvc.perform(post("/admin/users/10/reactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void reactivateUserErrorIfFails() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.reactivateUser(4L)).thenReturn(false);

        mvc.perform(post("/admin/users/4/reactivate").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Failed to reactivate user")));
    }

    @Test
    void reactivateUserSuccessRedirects() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.reactivateUser(5L)).thenReturn(true);

        mvc.perform(post("/admin/users/5/reactivate").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminUserService, times(1)).reactivateUser(5L);
    }

    // === banUser() ===

    @Test
    void banUserRedirectsIfNotAdmin() throws Exception {
        mvc.perform(post("/admin/users/5/ban"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void banUserErrorIfFails() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.banUser(5L)).thenReturn(false);

        mvc.perform(post("/admin/users/5/ban").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Failed to ban user")));
    }

    @Test
    void banUserSuccessRedirects() throws Exception {
        MockHttpSession session = createAdminSession();
        when(adminUserService.banUser(5L)).thenReturn(true);

        mvc.perform(post("/admin/users/5/ban").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminUserService, times(1)).banUser(5L);
    }

    // === listAllUsersByStatus() ===

    @Test
    void listAllUsersByStatusRedirectsIfNotAdmin() throws Exception {
        mvc.perform(get("/admin/users/filter"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void listAllUsersByStatusCallsGetAllUsersWhenBlankStatus() throws Exception {
        MockHttpSession session = createAdminSession();

        when(adminUserService.getAllUsers()).thenReturn(List.of(user(1, "a@x.com", "USER")));

        mvc.perform(get("/admin/users/filter")
                        .param("status", "")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("a@x.com")));

        verify(adminUserService, times(1)).getAllUsers();
        verify(adminUserService, never()).getUsersByStatus(any());
    }

    @Test
    void listAllUsersByStatusCallsGetUsersByStatusWhenProvided() throws Exception {
        MockHttpSession session = createAdminSession();

        when(adminUserService.getUsersByStatus("banned"))
                .thenReturn(List.of(user(2, "b@x.com", "USER")));

        mvc.perform(get("/admin/users/filter")
                        .param("status", "banned")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("b@x.com")));

        verify(adminUserService, times(1)).getUsersByStatus("banned");
        verify(adminUserService, never()).getAllUsers();
    }
}
