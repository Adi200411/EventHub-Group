package au.edu.rmit.sept.webapp.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import au.edu.rmit.sept.webapp.model.User;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GalleryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession authorizedSession;
    private MockHttpSession unauthorizedSession;
    private User mockOrganizer;

    @BeforeEach
    void setUp() {
        // Set up authorized session with organizer
        authorizedSession = new MockHttpSession();
        mockOrganizer = new User(1, "organizer@test.com", "hashedpassword", java.time.LocalDateTime.now());
        authorizedSession.setAttribute("user", mockOrganizer);

        // Set up unauthorized session
        unauthorizedSession = new MockHttpSession();
    }

    @Test
    void testGalleryViewRendering() throws Exception {
        // Test that gallery view renders properly with UI elements
        mockMvc.perform(get("/gallery")
                .session(authorizedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andExpect(model().attributeExists("photos"))
                .andExpect(content().string(containsString("Gallery")));
    }

    @Test
    void testSinglePhotoUploadValidation() throws Exception {
        // Test single photo upload with validation - redirects to specific event gallery
        MockMultipartFile validImage = new MockMultipartFile(
                "photo", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/gallery/upload")
                .file(validImage)
                .param("eventId", "1")
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gallery/event/*"));
    }

    @Test
    void testMultiplePhotoUploadValidation() throws Exception {
        // Test multiple photo upload with validation - redirects to specific event gallery
        MockMultipartFile validImage1 = new MockMultipartFile(
                "photos", "test1.jpg", "image/jpeg", "test image 1 content".getBytes());
        MockMultipartFile validImage2 = new MockMultipartFile(
                "photos", "test2.png", "image/png", "test image 2 content".getBytes());

        mockMvc.perform(multipart("/gallery/upload-multiple")
                .file(validImage1)
                .file(validImage2)
                .param("eventId", "1")
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gallery/event/*"));
    }

    @Test
    void testInvalidFileUploadRejection() throws Exception {
        // Test that invalid file types are rejected - redirects to specific event gallery
        MockMultipartFile invalidFile = new MockMultipartFile(
                "photo", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/gallery/upload")
                .file(invalidFile)
                .param("eventId", "1")
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gallery/event/*"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testFileSizeExceedsLimit() throws Exception {
        // Test that files exceeding size limit are rejected (simulate 30MB file)
        byte[] largeContent = new byte[30 * 1024 * 1024]; // 30MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "photo", "large.jpg", "image/jpeg", largeContent);

        mockMvc.perform(multipart("/gallery/upload")
                .file(largeFile)
                .param("eventId", "1")
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gallery/event/*"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testUnauthorizedPhotoDelete() throws Exception {
        // Attempt to delete photo without proper authentication/authorization
        mockMvc.perform(post("/gallery/delete/{photoId}", 1))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testUnauthorizedMultiplePhotoDelete() throws Exception {
        // Attempt to delete multiple photos without authorization
        mockMvc.perform(post("/gallery/delete-multiple")
                .param("photoIds", "1", "2", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gallery"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testAuthorizedSinglePhotoDelete() throws Exception {
        // Test deleting a single photo with proper authorization
        mockMvc.perform(post("/gallery/delete/{photoId}", 1)
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testAuthorizedMultiplePhotoDelete() throws Exception {
        // Test deleting multiple photos with proper authorization
        mockMvc.perform(post("/gallery/delete-multiple")
                .param("photoIds", "1", "2", "3")
                .session(authorizedSession))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testGalleryImageLoading() throws Exception {
        // Test that gallery loads and displays images properly
        MvcResult result = mockMvc.perform(get("/gallery")
                .session(authorizedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery-view"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // Verify HTML structure for image display
        assertTrue(content.contains("<img") || content.contains("No photos"));
    }
}
