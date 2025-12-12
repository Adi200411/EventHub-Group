package au.edu.rmit.sept.webapp.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Event_Photos;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.PhotoService;

@SpringBootTest
@AutoConfigureMockMvc
class GalleryViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoService photoService;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private User organizerUser;
    private Event testEvent;
    private Event_Photos testPhoto;
    private List<Event_Photos> testPhotos;
    private MockHttpSession sessionWithUser;
    private MockHttpSession sessionWithOrganizer;

    @BeforeEach
    void setUp() {
        // Create test user (not an organizer)
        testUser = new User(1, "user@test.com", "password", LocalDateTime.now());
        
        // Create test organizer user
        organizerUser = new User(2, "organizer@test.com", "password", LocalDateTime.now());

        // Create test event
        testEvent = new Event(
            101, 2L, 3L,
            "Test Event",
            "Test Description",
            "Test Venue",
            java.time.LocalDate.now().plusDays(1),
            java.time.LocalDateTime.now().plusDays(1),
            "20:00",
            "upcoming",
            100
        );

        // Create test photo
        testPhoto = new Event_Photos(
            1, 101, 2, "/event_photos/test.jpg", LocalDateTime.now()
        );

        testPhotos = Arrays.asList(testPhoto);

        // Create sessions
        sessionWithUser = new MockHttpSession();
        sessionWithUser.setAttribute("user", testUser);

        sessionWithOrganizer = new MockHttpSession();
        sessionWithOrganizer.setAttribute("user", organizerUser);
    }

    // View Tests
    @Test
    void viewEventGallery_WithoutSession_Success() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenReturn(testPhotos);
        when(eventService.getAllEvents()).thenReturn(Arrays.asList(testEvent));

        // Act & Assert
        mockMvc.perform(get("/gallery/event/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attribute("photos", testPhotos))
                .andExpect(model().attribute("selectedEventId", 101))
                .andExpect(model().attribute("canUpload", false))
                .andExpect(model().attribute("authenticated", false));

        verify(photoService, times(1)).getEventPhotos(101);
        verify(eventService, times(1)).getAllEvents();
    }

    @Test
    void viewEventGallery_WithUserSession_Success() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenReturn(testPhotos);
        when(eventService.getAllEvents()).thenReturn(Arrays.asList(testEvent));
        when(userRepository.getOrganiserIdByUserId(1)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/gallery/event/101").session(sessionWithUser))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attribute("photos", testPhotos))
                .andExpect(model().attribute("selectedEventId", 101))
                .andExpect(model().attribute("canUpload", false))
                .andExpect(model().attribute("authenticated", true))
                .andExpect(model().attribute("currentUserEmail", "user@test.com"));

        verify(userRepository, times(1)).getOrganiserIdByUserId(1);
    }

    @Test
    void viewEventGallery_WithOrganizerSession_CanUpload() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenReturn(testPhotos);
        when(eventService.getAllEvents()).thenReturn(Arrays.asList(testEvent));
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/gallery/event/101").session(sessionWithOrganizer))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attribute("canUpload", true))
                .andExpect(model().attribute("authenticated", true));

        verify(userRepository, times(1)).isOrganiserForEvent(2, 101);
    }

    @Test
    void viewEventGallery_ServiceException_ReturnsError() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/gallery/event/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "Failed to load event gallery"));
    }

    @Test
    void viewGallery_WithEventId_Success() throws Exception {
        // Arrange  
        when(photoService.getEventPhotos(101)).thenReturn(testPhotos);
        when(eventService.getAllEvents()).thenReturn(Arrays.asList(testEvent));

        // Act & Assert
        mockMvc.perform(get("/gallery").param("eventId", "101"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attribute("photos", testPhotos))
                .andExpect(model().attribute("selectedEventId", 101));
    }

    @Test
    void viewGallery_WithoutEventId_ShowsAllPhotos() throws Exception {
        // Arrange
        when(photoService.getAllPhotos()).thenReturn(testPhotos);
        when(eventService.getAllEvents()).thenReturn(Arrays.asList(testEvent));

        // Act & Assert
        mockMvc.perform(get("/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attribute("photos", testPhotos))
                .andExpect(model().attributeDoesNotExist("selectedEventId"));

        verify(photoService, times(1)).getAllPhotos();
    }

    // Upload Tests
    @Test
    void uploadPhoto_WithoutSession_Unauthorized() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));

        verify(photoService, never()).uploadPhoto(anyInt(), anyInt(), any());
    }

    @Test
    void uploadPhoto_WithNonOrganizerUser_Unauthorized() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );
        when(userRepository.getOrganiserIdByUserId(1)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101")
                .session(sessionWithUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));

        verify(photoService, never()).uploadPhoto(anyInt(), anyInt(), any());
    }

    @Test
    void uploadPhoto_WithOrganizer_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);
        when(photoService.uploadPhoto(101, 2, file)).thenReturn(testPhoto);

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101")
                .session(sessionWithOrganizer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("flashMessage", "Photo uploaded successfully!"));

        verify(photoService, times(1)).uploadPhoto(101, 2, file);
    }

    @Test
    void uploadPhoto_InvalidFile_ReturnsError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);
        when(photoService.uploadPhoto(101, 2, file))
            .thenThrow(new IllegalArgumentException("Invalid file format"));

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101")
                .session(sessionWithOrganizer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", "Invalid file format"));
    }

    @Test
    void uploadMultiplePhotos_WithOrganizer_Success() throws Exception {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
            "photos", "test1.jpg", "image/jpeg", "test content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "photos", "test2.jpg", "image/jpeg", "test content 2".getBytes()
        );
        
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);
        when(photoService.uploadMultiplePhotos(eq(101), eq(2), anyList())).thenReturn(testPhotos);

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload-multiple")
                .file(file1)
                .file(file2)
                .param("eventId", "101")
                .session(sessionWithOrganizer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("flashMessage", "1 photo(s) uploaded successfully!"));

        verify(photoService, times(1)).uploadMultiplePhotos(eq(101), eq(2), anyList());
    }

    // Delete Tests
    @Test
    void deletePhoto_WithoutSession_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/gallery/delete/1")
                .param("eventId", "101")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", containsString("must be logged in")));

        verify(photoService, never()).deletePhoto(anyInt());
    }

    @Test
    void deletePhoto_WithNonOrganizerUser_Unauthorized() throws Exception {
        // Arrange
        when(photoService.getPhotoById(1)).thenReturn(Optional.of(testPhoto));
        when(userRepository.getOrganiserIdByUserId(1)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/gallery/delete/1")
                .param("eventId", "101")
                .session(sessionWithUser)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));

        verify(photoService, never()).deletePhoto(anyInt());
    }

    @Test
    void deletePhoto_WithOrganizer_Success() throws Exception {
        // Arrange
        when(photoService.getPhotoById(1)).thenReturn(Optional.of(testPhoto));
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/gallery/delete/1")
                .param("eventId", "101")
                .session(sessionWithOrganizer)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("flashMessage", "Photo deleted successfully!"));

        verify(photoService, times(1)).deletePhoto(1);
    }

    @Test
    void deletePhoto_PhotoNotFound_ReturnsError() throws Exception {
        // Arrange
        when(photoService.getPhotoById(999)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/gallery/delete/999")
                .param("eventId", "101")
                .session(sessionWithOrganizer)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", "Photo not found"));

        verify(photoService, never()).deletePhoto(anyInt());
    }

    @Test
    void deleteMultiplePhotos_WithOrganizer_Success() throws Exception {
        // Arrange
        when(photoService.getPhotoById(1)).thenReturn(Optional.of(testPhoto));
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/gallery/delete-multiple")
                .param("photoIds", "1")
                .param("eventId", "101")
                .session(sessionWithOrganizer)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("flashMessage", "1 photo(s) deleted successfully!"));

        verify(photoService, times(1)).deleteMultiplePhotos(Arrays.asList(1));
    }

    @Test
    void deleteMultiplePhotos_WithoutSession_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/gallery/delete-multiple")
                .param("photoIds", "1")
                .param("eventId", "101")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery/event/101"))
                .andExpect(flash().attribute("errorMessage", containsString("must be logged in")));

        verify(photoService, never()).deleteMultiplePhotos(anyList());
    }

    @Test
    void deleteMultiplePhotos_NoPhotosSelected_ReturnsError() throws Exception {
        // Act & Assert - When required photoIds parameter is missing, Spring returns 400 Bad Request
        mockMvc.perform(post("/gallery/delete-multiple")
                .param("eventId", "101") 
                .session(sessionWithOrganizer)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest());

        verify(photoService, never()).deleteMultiplePhotos(anyList());
    }

    // API Tests
    @Test
    void getEventPhotosApi_Success() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenReturn(testPhotos);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/gallery/api/events/101/photos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn(); 

        // Verify the JSON response contains the photo data
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("test.jpg"));
    }

    @Test
    void getEventPhotosApi_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(photoService.getEventPhotos(101)).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/gallery/api/events/101/photos"))
                .andExpect(status().isInternalServerError());
    }

    // Authorization Helper Tests
    @Test
    void canUploadToEvent_UserNotLoggedIn_ReturnsFalse() throws Exception {
        // This tests the private method indirectly through the upload endpoint
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()    
        );

        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101"))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));
    }

    @Test
    void canUploadToEvent_UserNotOrganizer_ReturnsFalse() throws Exception {
        // Arrange - user exists but is not an organizer
        when(userRepository.getOrganiserIdByUserId(1)).thenReturn(Optional.empty());
        
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101")
                .session(sessionWithUser))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));
    }

    @Test
    void canUploadToEvent_OrganizerNotForThisEvent_ReturnsFalse() throws Exception {
        // Arrange - user is organizer but not for this event
        when(userRepository.getOrganiserIdByUserId(2)).thenReturn(Optional.of(2));
        when(userRepository.isOrganiserForEvent(2, 101)).thenReturn(false);
        
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/gallery/upload")
                .file(file)
                .param("eventId", "101")
                .session(sessionWithOrganizer))
                .andExpect(flash().attribute("errorMessage", containsString("not authorized")));
    }
}