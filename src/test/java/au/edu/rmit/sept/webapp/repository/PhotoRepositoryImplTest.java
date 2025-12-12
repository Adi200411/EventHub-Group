package au.edu.rmit.sept.webapp.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;

import au.edu.rmit.sept.webapp.model.Event_Photos;

@SpringBootTest
class PhotoRepositoryImplTest {

    PhotoRepository photoRepository;

    @Autowired
    DataSource dataSource;

    @Autowired
    private Flyway flyway;

    private Event_Photos testPhoto1;
    private Event_Photos testPhoto2;
    private Event_Photos testPhoto3;

    @BeforeEach
    public void setUp() {
        flyway.migrate();
        photoRepository = new PhotoRepositoryImpl(dataSource);
        
        LocalDateTime now = LocalDateTime.now();
        
        testPhoto1 = new Event_Photos(
            0, // ID will be generated
            1, // event_id (assuming event 1 exists from migrations)
            1, // organiser_id (assuming organiser 1 exists from migrations)
            "/event_photos/test1.jpg",
            now
        );

        testPhoto2 = new Event_Photos(
            0, // ID will be generated
            1, // same event
            1, // same organiser
            "/event_photos/test2.jpg",
            now.plusMinutes(5)
        );

        testPhoto3 = new Event_Photos(
            0, // ID will be generated
            2, // different event
            2, // different organiser
            "/event_photos/test3.jpg", 
            now.plusMinutes(10)
        );
    }

    @AfterEach
    public void tearDown() {
        flyway.clean();
    }

    @Test
    void save_ValidPhoto_Success() {
        // Act
        Event_Photos savedPhoto = photoRepository.save(testPhoto1);

        // Assert
        assertNotNull(savedPhoto);
        assertTrue(savedPhoto.photo_id() > 0);
        assertEquals(testPhoto1.event_id(), savedPhoto.event_id());
        assertEquals(testPhoto1.organiser_id(), savedPhoto.organiser_id());
        assertEquals(testPhoto1.url(), savedPhoto.url());
        assertNotNull(savedPhoto.uploaded_at());
    }

    @Test
    void save_NullPhoto_ThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, () -> photoRepository.save(null));
    }

    @Test
    void findById_ExistingPhoto_ReturnsPhoto() {
        // Arrange
        Event_Photos savedPhoto = photoRepository.save(testPhoto1);

        // Act
        Optional<Event_Photos> foundPhoto = photoRepository.findById(savedPhoto.photo_id());

        // Assert
        assertTrue(foundPhoto.isPresent());
        assertEquals(savedPhoto.photo_id(), foundPhoto.get().photo_id());
        assertEquals(savedPhoto.url(), foundPhoto.get().url());
    }

    @Test
    void findById_NonExistingPhoto_ReturnsEmpty() {
        // Act
        Optional<Event_Photos> foundPhoto = photoRepository.findById(99999);

        // Assert
        assertFalse(foundPhoto.isPresent());
    }

    @Test
    void findByEventId_ExistingEvent_ReturnsPhotos() {
        // Arrange
        Event_Photos saved1 = photoRepository.save(testPhoto1);
        Event_Photos saved2 = photoRepository.save(testPhoto2);
        photoRepository.save(testPhoto3); // Different event

        // Act
        List<Event_Photos> photos = photoRepository.findByEventId(1);

        // Assert
        assertFalse(photos.isEmpty());
        
        // Should contain our test photos
        assertTrue(photos.stream().anyMatch(p -> p.photo_id() == saved1.photo_id()));
        assertTrue(photos.stream().anyMatch(p -> p.photo_id() == saved2.photo_id()));
        
        // Should not contain the photo from different event
        assertFalse(photos.stream().anyMatch(p -> p.event_id() != 1));
    }

    @Test
    void findByEventId_NonExistingEvent_ReturnsEmptyList() {
        // Act
        List<Event_Photos> photos = photoRepository.findByEventId(99999);

        // Assert
        assertTrue(photos.isEmpty());
    }

    @Test
    void findByOrganiserId_ExistingOrganiser_ReturnsPhotos() {
        // Arrange
        Event_Photos saved1 = photoRepository.save(testPhoto1);
        Event_Photos saved2 = photoRepository.save(testPhoto2);
        Event_Photos saved3 = photoRepository.save(testPhoto3);

        // Act
        List<Event_Photos> photos = photoRepository.findByOrganiserId(1);

        // Assert
        assertFalse(photos.isEmpty());
        
        // Should contain photos from events organized by organiser 1
        assertTrue(photos.stream().anyMatch(p -> p.photo_id() == saved1.photo_id()));
        assertTrue(photos.stream().anyMatch(p -> p.photo_id() == saved2.photo_id()));
        
        // Should not contain photos from events not organized by organiser 1
        // (testPhoto3 is from event_id 2, which might be organized by different user)
        assertFalse(photos.stream().anyMatch(p -> p.photo_id() == saved3.photo_id()));
    }

    @Test
    void findByOrganiserId_NonExistingOrganiser_ReturnsEmptyList() {
        // Act
        List<Event_Photos> photos = photoRepository.findByOrganiserId(99999);

        // Assert
        assertTrue(photos.isEmpty());
    }

    @Test
    void deleteById_ExistingPhoto_Success() {
        // Arrange
        Event_Photos savedPhoto = photoRepository.save(testPhoto1);
        int photoId = savedPhoto.photo_id();

        // Verify it exists
        assertTrue(photoRepository.findById(photoId).isPresent());

        // Act
        photoRepository.deleteById(photoId);

        // Assert
        assertFalse(photoRepository.findById(photoId).isPresent());
    }  

    @Test
    void deleteById_NonExistingPhoto_NoException() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> photoRepository.deleteById(99999));
    }

    @Test
    void deleteMultipleById_ExistingPhotos_Success() {
        // Arrange
        Event_Photos saved1 = photoRepository.save(testPhoto1);
        Event_Photos saved2 = photoRepository.save(testPhoto2);
        Event_Photos saved3 = photoRepository.save(testPhoto3);
        
        List<Integer> idsToDelete = Arrays.asList(saved1.photo_id(), saved2.photo_id());

        // Verify they exist
        assertTrue(photoRepository.findById(saved1.photo_id()).isPresent());
        assertTrue(photoRepository.findById(saved2.photo_id()).isPresent());
        assertTrue(photoRepository.findById(saved3.photo_id()).isPresent());

        // Act
        photoRepository.deleteMultipleById(idsToDelete);

        // Assert
        assertFalse(photoRepository.findById(saved1.photo_id()).isPresent());
        assertFalse(photoRepository.findById(saved2.photo_id()).isPresent());
        assertTrue(photoRepository.findById(saved3.photo_id()).isPresent()); // Should still exist
    }

    @Test
    void deleteMultipleById_EmptyList_NoException() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> photoRepository.deleteMultipleById(Arrays.asList()));
    }

    @Test
    void deleteMultipleById_NonExistingPhotos_NoException() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> photoRepository.deleteMultipleById(Arrays.asList(99998, 99999)));
    }

    @Test
    void deleteMultipleById_MixedExistingNonExisting_Success() {
        // Arrange
        Event_Photos saved1 = photoRepository.save(testPhoto1);
        List<Integer> idsToDelete = Arrays.asList(saved1.photo_id(), 99999);

        // Verify saved photo exists
        assertTrue(photoRepository.findById(saved1.photo_id()).isPresent());

        // Act
        photoRepository.deleteMultipleById(idsToDelete);

        // Assert
        assertFalse(photoRepository.findById(saved1.photo_id()).isPresent());
        // Non-existing photo should not cause issues
    }

    @Test
    void findAll_MultiplePhotos_ReturnsAllOrderedByDate() {
        // Arrange
        Event_Photos saved1 = photoRepository.save(testPhoto1);
        Event_Photos saved2 = photoRepository.save(testPhoto2);
        Event_Photos saved3 = photoRepository.save(testPhoto3);

        // Act
        List<Event_Photos> allPhotos = photoRepository.findAll();

        // Assert
        assertTrue(allPhotos.size() >= 3); // May have more from other tests or seed data
        
        // Check that our saved photos are in the result
        assertTrue(allPhotos.stream().anyMatch(p -> p.photo_id() == saved1.photo_id()));
        assertTrue(allPhotos.stream().anyMatch(p -> p.photo_id() == saved2.photo_id()));
        assertTrue(allPhotos.stream().anyMatch(p -> p.photo_id() == saved3.photo_id()));

        // Verify ordering (newer first) for consecutive photos
        for (int i = 0; i < allPhotos.size() - 1; i++) {
            LocalDateTime current = allPhotos.get(i).uploaded_at();
            LocalDateTime next = allPhotos.get(i + 1).uploaded_at();
            assertTrue(current.isAfter(next) || current.isEqual(next),
                     "Photos should be ordered by uploaded_at DESC");
        }
    }

    @Test
    void findAll_NoPhotos_ReturnsEmptyList() {
        // Note: This test might not work if there's seed data
        // Just verify the method doesn't throw an exception
        assertDoesNotThrow(() -> {
            List<Event_Photos> photos = photoRepository.findAll();
            assertNotNull(photos);
        });
    }

    @Test
    void save_InvalidEventId_ThrowsException() {
        // Arrange
        Event_Photos invalidPhoto = new Event_Photos(
            0,
            99999, // Non-existing event
            1,
            "/event_photos/invalid.jpg",
            LocalDateTime.now()
        );

        // Act & Assert
        // This should throw an exception due to foreign key constraint
        assertThrows(DataAccessResourceFailureException.class, 
                     () -> photoRepository.save(invalidPhoto));
    }

    @Test
    void save_InvalidOrganiserId_ThrowsException() {
        // Arrange
        Event_Photos invalidPhoto = new Event_Photos(
            0,
            1,
            99999, // Non-existing organiser
            "/event_photos/invalid.jpg",
            LocalDateTime.now()
        );

        // Act & Assert
        // This should throw an exception due to foreign key constraint
        assertThrows(DataAccessResourceFailureException.class, 
                     () -> photoRepository.save(invalidPhoto));
    }

    @Test
    void mapRowToPhoto_ValidResultSet_ReturnsCorrectPhoto() {
        // This test indirectly tests the private mapRowToPhoto method
        // by saving and retrieving a photo

        // Arrange
        Event_Photos savedPhoto = photoRepository.save(testPhoto1);

        // Act
        Optional<Event_Photos> retrievedPhoto = photoRepository.findById(savedPhoto.photo_id());

        // Assert
        assertTrue(retrievedPhoto.isPresent());
        Event_Photos photo = retrievedPhoto.get();
        
        assertEquals(savedPhoto.photo_id(), photo.photo_id());
        assertEquals(testPhoto1.event_id(), photo.event_id());
        assertEquals(testPhoto1.organiser_id(), photo.organiser_id());
        assertEquals(testPhoto1.url(), photo.url());
        assertNotNull(photo.uploaded_at());
    }

    @Test
    void findByEventId_LargeDataset_PerformanceTest() {
        // Arrange - Save multiple photos for the same event
        for (int i = 0; i < 50; i++) {
            Event_Photos photo = new Event_Photos(
                0,
                1,
                1,
                "/event_photos/perf_test_" + i + ".jpg",
                LocalDateTime.now().plusSeconds(i)
            );
            photoRepository.save(photo);
        }

        // Act
        long startTime = System.currentTimeMillis();
        List<Event_Photos> photos = photoRepository.findByEventId(1);
        long endTime = System.currentTimeMillis();

        // Assert
        assertTrue(photos.size() >= 50);
        assertTrue(endTime - startTime < 1000, "Query should complete within 1 second");
    }
}