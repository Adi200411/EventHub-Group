package au.edu.rmit.sept.webapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import au.edu.rmit.sept.webapp.model.Event_Photos;
import au.edu.rmit.sept.webapp.repository.PhotoRepository;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private PhotoServiceImpl photoService;

    private Event_Photos testPhoto;
    private MockMultipartFile validImageFile;
    private MockMultipartFile invalidFile;
    private MockMultipartFile largeFile;
    private MockMultipartFile emptyFile;

    @BeforeEach
    void setUp() {
        // Set upload directory for testing
        ReflectionTestUtils.setField(photoService, "uploadDir", "test-uploads");
        
        testPhoto = new Event_Photos(
            1,
            101,
            201,
            "/event_photos/test-image.jpg",
            LocalDateTime.now()
        );

        // Create test files
        validImageFile = new MockMultipartFile(
            "photo",
            "test-image.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        invalidFile = new MockMultipartFile(
            "photo",
            "test-document.txt",
            "text/plain",
            "fake text content".getBytes()
        );

        // Create a file larger than 25MB
        byte[] largeContent = new byte[26 * 1024 * 1024]; // 26MB
        largeFile = new MockMultipartFile(
            "photo",
            "large-image.jpg",
            "image/jpeg",
            largeContent
        );

        emptyFile = new MockMultipartFile(
            "photo",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );
    }

    @Test
    void uploadPhoto_ValidFile_Success() {
        // Arrange
        when(photoRepository.save(any(Event_Photos.class))).thenReturn(testPhoto);

        // Act
        Event_Photos result = photoService.uploadPhoto(101, 201, validImageFile);

        // Assert
        assertNotNull(result);
        assertEquals(testPhoto.event_id(), result.event_id());
        assertEquals(testPhoto.organiser_id(), result.organiser_id());
        verify(photoRepository, times(1)).save(any(Event_Photos.class));
    }

    @Test
    void uploadPhoto_EmptyFile_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> photoService.uploadPhoto(101, 201, emptyFile)
        );
        
        assertEquals("File cannot be empty", exception.getMessage());
        verify(photoRepository, never()).save(any(Event_Photos.class));
    }

    @Test
    void uploadPhoto_InvalidFileFormat_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> photoService.uploadPhoto(101, 201, invalidFile)
        );
        
        assertEquals("Invalid image file format", exception.getMessage());
        verify(photoRepository, never()).save(any(Event_Photos.class));
    }

    @Test
    void uploadPhoto_FileTooLarge_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> photoService.uploadPhoto(101, 201, largeFile)
        );
        
        assertEquals("File size exceeds maximum limit of 25MB", exception.getMessage());
        verify(photoRepository, never()).save(any(Event_Photos.class));
    }

    @Test
    void uploadMultiplePhotos_ValidFiles_Success() {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile("photo1", "image1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("photo2", "image2.png", "image/png", "content2".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);

        Event_Photos photo1 = new Event_Photos(1, 101, 201, "/event_photos/image1.jpg", LocalDateTime.now());
        Event_Photos photo2 = new Event_Photos(2, 101, 201, "/event_photos/image2.png", LocalDateTime.now());

        when(photoRepository.save(any(Event_Photos.class)))
            .thenReturn(photo1)
            .thenReturn(photo2);

        // Act
        List<Event_Photos> result = photoService.uploadMultiplePhotos(101, 201, files);

        // Assert
        assertEquals(2, result.size());
        verify(photoRepository, times(2)).save(any(Event_Photos.class));
    }

    @Test
    void uploadMultiplePhotos_EmptyList_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> photoService.uploadMultiplePhotos(101, 201, Arrays.asList())
        );
        
        assertEquals("No files provided for upload", exception.getMessage());
    }

    @Test
    void uploadMultiplePhotos_AllEmptyFiles_ThrowsException() {
        // Arrange
        List<MultipartFile> emptyFiles = Arrays.asList(emptyFile, emptyFile);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> photoService.uploadMultiplePhotos(101, 201, emptyFiles)
        );
        
        assertEquals("All files are empty", exception.getMessage());
    }

    @Test
    void uploadMultiplePhotos_MixedValidInvalid_PartialSuccess() {
        // Arrange
        List<MultipartFile> mixedFiles = Arrays.asList(validImageFile, invalidFile);
        when(photoRepository.save(any(Event_Photos.class))).thenReturn(testPhoto);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> photoService.uploadMultiplePhotos(101, 201, mixedFiles)
        );
        
        assertTrue(exception.getMessage().contains("Some files failed to upload"));
        assertTrue(exception.getMessage().contains("Successfully uploaded 1 file(s)"));
    }

    @Test
    void isValidImageFile_ValidExtensions_ReturnsTrue() {
        // Test all valid extensions
        String[] validExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        
        for (String ext : validExtensions) {
            MockMultipartFile file = new MockMultipartFile(
                "photo", 
                "test" + ext, 
                "image/jpeg", 
                "content".getBytes()
            );
            assertTrue(photoService.isValidImageFile(file), "Should accept " + ext);
        }
    }

    @Test
    void isValidImageFile_InvalidExtensions_ReturnsFalse() {
        // Test invalid extensions
        String[] invalidExtensions = {".txt", ".pdf", ".doc", ".exe"};
        
        for (String ext : invalidExtensions) {
            MockMultipartFile file = new MockMultipartFile(
                "photo", 
                "test" + ext, 
                "text/plain", 
                "content".getBytes()
            );
            assertFalse(photoService.isValidImageFile(file), "Should reject " + ext);
        }
    }

    @Test
    void isValidImageFile_EmptyFile_ReturnsFalse() {
        assertFalse(photoService.isValidImageFile(emptyFile));
    }

    @Test
    void isValidImageFile_NullFilename_ReturnsFalse() {
        MockMultipartFile fileWithNullName = new MockMultipartFile(
            "photo", 
            null, 
            "image/jpeg", 
            "content".getBytes()
        );
        assertFalse(photoService.isValidImageFile(fileWithNullName));
    }

    @Test
    void getEventPhotos_Success() {
        // Arrange
        List<Event_Photos> expectedPhotos = Arrays.asList(testPhoto);
        when(photoRepository.findByEventId(101)).thenReturn(expectedPhotos);

        // Act
        List<Event_Photos> result = photoService.getEventPhotos(101);

        // Assert
        assertEquals(expectedPhotos, result);
        verify(photoRepository, times(1)).findByEventId(101);
    }

    @Test
    void getAllPhotos_Success() {
        // Arrange
        List<Event_Photos> expectedPhotos = Arrays.asList(testPhoto);
        when(photoRepository.findAll()).thenReturn(expectedPhotos);

        // Act
        List<Event_Photos> result = photoService.getAllPhotos();

        // Assert
        assertEquals(expectedPhotos, result);
        verify(photoRepository, times(1)).findAll();
    }

    @Test
    void getPhotoById_Found() {
        // Arrange
        when(photoRepository.findById(1)).thenReturn(Optional.of(testPhoto));

        // Act
        Optional<Event_Photos> result = photoService.getPhotoById(1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testPhoto, result.get());
        verify(photoRepository, times(1)).findById(1);
    }

    @Test
    void getPhotoById_NotFound() {
        // Arrange
        when(photoRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        Optional<Event_Photos> result = photoService.getPhotoById(999);

        // Assert
        assertFalse(result.isPresent());
        verify(photoRepository, times(1)).findById(999);
    }

    @Test
    void deletePhoto_PhotoExists() {
        // Arrange
        when(photoRepository.findById(1)).thenReturn(Optional.of(testPhoto));

        // Act
        photoService.deletePhoto(1);

        // Assert
        verify(photoRepository, times(1)).findById(1);
        verify(photoRepository, times(1)).deleteById(1);
    }

    @Test
    void deletePhoto_PhotoNotExists() {
        // Arrange
        when(photoRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        photoService.deletePhoto(999);

        // Assert
        verify(photoRepository, times(1)).findById(999);
        verify(photoRepository, never()).deleteById(999);
    }

    @Test
    void deleteMultiplePhotos_Success() {
        // Arrange
        List<Integer> photoIds = Arrays.asList(1, 2);
        Event_Photos photo1 = new Event_Photos(1, 101, 201, "/event_photos/photo1.jpg", LocalDateTime.now());
        Event_Photos photo2 = new Event_Photos(2, 101, 201, "/event_photos/photo2.jpg", LocalDateTime.now());
        
        when(photoRepository.findById(1)).thenReturn(Optional.of(photo1));
        when(photoRepository.findById(2)).thenReturn(Optional.of(photo2));

        // Act
        photoService.deleteMultiplePhotos(photoIds);

        // Assert
        verify(photoRepository, times(1)).findById(1);
        verify(photoRepository, times(1)).findById(2);
        verify(photoRepository, times(1)).deleteMultipleById(photoIds);
    }

    @Test
    void deleteMultiplePhotos_EmptyList_NoAction() {
        // Act
        photoService.deleteMultiplePhotos(Arrays.asList());

        // Assert
        verifyNoInteractions(photoRepository);
    }

    @Test
    void deleteMultiplePhotos_NullList_NoAction() {
        // Act
        photoService.deleteMultiplePhotos(null);

        // Assert
        verifyNoInteractions(photoRepository);
    }

    @Test
    void getEventPhotoThumbnails_Success() {
        // Arrange
        List<Event_Photos> allPhotos = Arrays.asList(
            new Event_Photos(1, 101, 201, "/photo1.jpg", LocalDateTime.now()),
            new Event_Photos(2, 101, 201, "/photo2.jpg", LocalDateTime.now()),
            new Event_Photos(3, 101, 201, "/photo3.jpg", LocalDateTime.now()),
            new Event_Photos(4, 101, 201, "/photo4.jpg", LocalDateTime.now())
        );
        when(photoRepository.findByEventId(101)).thenReturn(allPhotos);

        // Act
        List<Event_Photos> result = photoService.getEventPhotoThumbnails(101, 2);

        // Assert
        assertEquals(2, result.size());
        verify(photoRepository, times(1)).findByEventId(101);
    }

    @Test
    void getEventPhotoThumbnails_ExceptionHandling() {
        // Arrange
        when(photoRepository.findByEventId(101)).thenThrow(new RuntimeException("Database error"));

        // Act
        List<Event_Photos> result = photoService.getEventPhotoThumbnails(101, 2);

        // Assert
        assertTrue(result.isEmpty());
        verify(photoRepository, times(1)).findByEventId(101);
    }
}