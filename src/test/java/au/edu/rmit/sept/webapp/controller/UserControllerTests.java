package au.edu.rmit.sept.webapp.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import au.edu.rmit.sept.webapp.service.ClubService;
import au.edu.rmit.sept.webapp.service.OrganiserService;
import au.edu.rmit.sept.webapp.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
  
  @Autowired
  private MockMvc mvc;

  @MockBean
  private UserService userService;
  
  @MockBean
  private OrganiserService organiserService;
  
  @MockBean
  private ClubService clubService;

  // deleteUser tests

  @Test
  void deleteUser_id10_callsService_andReturns200() throws Exception {
    mvc.perform(delete("/users/10"))
        .andExpect(status().isOk());

    verify(userService, times(1)).deleteUser(10L);
  }

  @Test
  void deleteUser_id77_callsService_andReturns200() throws Exception {
    mvc.perform(delete("/users/77"))
        .andExpect(status().isOk());

    verify(userService, times(1)).deleteUser(77L);
  }

  // SaveUser tests
 
//  @Test
//  void register_withSessionUserId_callsSaveUser_withThatId_andReturnsBodyAccountSetup() throws Exception {
//    MockHttpSession session = new MockHttpSession();
//    session.setAttribute("userId", 42); // Integer, as controller sets/reads
//
//    mvc.perform(post("/users/register")
//            .session(session)
//            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//            .param("name", "Ada")
//            .param("course", "CS")
//            .param("interest", "AI"))
//        .andExpect(status().isFound())
//        .andExpect(content().string("account-setup"));
//
//    verify(userService, times(1)).saveUser(42, "Ada", "CS", "AI");
//  }

  // UpdateUser tests

  @Test
  void update_callsService_andReturnsBodyAccountSetup() throws Exception {
    mvc.perform(put("/users/update")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", "5")
            .param("name", "Carol")
            .param("course", "SE")
            .param("interest", "Web"))
        .andExpect(status().isOk())
        .andExpect(content().string("account-setup"));

    verify(userService, times(1)).updateUser(5, "Carol", "SE", "Web");
  }

  @Test
  void update_withDifferentValues_callsService_andReturnsBodyAccountSetup() throws Exception {
    mvc.perform(put("/users/update")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", "6")
            .param("name", "Dan")
            .param("course", "DS")
            .param("interest", "ML"))
        .andExpect(status().isOk())
        .andExpect(content().string("account-setup"));

    verify(userService, times(1)).updateUser(6, "Dan", "DS", "ML");
  }

  // DeleteProfile tests

  @Test
  void deleteProfile_callsService_andReturnsBodySignup() throws Exception {
    mvc.perform(delete("/users/delete")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", "12"))
        .andExpect(status().isOk())
        .andExpect(content().string("signup"));

    verify(userService, times(1)).deleteProfile(12);
  }

  @Test
  void deleteProfile_withAnotherId_callsService_andReturnsBodySignup() throws Exception {
    mvc.perform(delete("/users/delete")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("id", "99"))
        .andExpect(status().isOk())
        .andExpect(content().string("signup"));

    verify(userService, times(1)).deleteProfile(99);
  }
}
