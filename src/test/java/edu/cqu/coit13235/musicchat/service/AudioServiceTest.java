package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AudioService.
 * Tests file upload, storage, and retrieval functionality.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class AudioServiceTest {
    
    @Mock
    private AudioTrackRepository audioTrackRepository;
    
    @Mock
    private UserRepository userRepository;
    
    private AudioService audioService;
    
    private MockMultipartFile validAudioFile;
    private MockMultipartFile invalidFile;
    private AudioTrack sampleTrack;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("testuser", "test@example.com", "password");
        
        // Initialize AudioService with mock dependencies
        audioService = new AudioService(audioTrackRepository, userRepository, "test-uploads", 52428800L);
        
        // Create a valid audio file for testing
        validAudioFile = new MockMultipartFile(
            "file",
            "test-song.mp3",
            "audio/mpeg",
            "test audio content".getBytes()
        );
        
        // Create an invalid file for testing
        invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );
        
        // Create a sample track
        sampleTrack = new AudioTrack(
            "Test Song",
            "Test Artist",
            "test-song.mp3",
            "test-song.mp3",
            180, // 3 minutes
            1024L,
            "audio/mpeg",
            testUser
        );
        sampleTrack.setId(1L);
        sampleTrack.setUploadedAt(LocalDateTime.now());
    }
    
    @Test
    void uploadAudio_ValidFile_ShouldReturnAudioTrack() throws IOException {
        // Arrange
        when(audioTrackRepository.save(any(AudioTrack.class))).thenReturn(sampleTrack);
        
        // Act
        AudioTrack result = audioService.uploadAudio(validAudioFile, "Test Song", "Test Artist", testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Song", result.getTitle());
        assertEquals("Test Artist", result.getArtist());
        assertEquals("audio/mpeg", result.getContentType());
        assertEquals(1024L, result.getFileSizeBytes());
        assertNotNull(result.getFilename());
        assertTrue(result.getFilename().endsWith(".mp3"));
        
        verify(audioTrackRepository, times(1)).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_EmptyFile_ShouldThrowException() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.mp3",
            "audio/mpeg",
            new byte[0]
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(emptyFile, "Test Song", "Test Artist");
        });
        
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_InvalidContentType_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(invalidFile, "Test Song", "Test Artist");
        });
        
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_NullTitle_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(validAudioFile, null, "Test Artist");
        });
        
        assertEquals("Title cannot be null or empty", exception.getMessage());
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_EmptyTitle_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(validAudioFile, "", "Test Artist");
        });
        
        assertEquals("Title cannot be null or empty", exception.getMessage());
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_NullArtist_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(validAudioFile, "Test Song", null);
        });
        
        assertEquals("Artist cannot be null or empty", exception.getMessage());
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void uploadAudio_EmptyArtist_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioService.uploadAudio(validAudioFile, "Test Song", "");
        });
        
        assertEquals("Artist cannot be null or empty", exception.getMessage());
        verify(audioTrackRepository, never()).save(any(AudioTrack.class));
    }
    
    @Test
    void getAllTracks_ShouldReturnAllTracks() {
        // Arrange
        List<AudioTrack> expectedTracks = Arrays.asList(sampleTrack);
        when(audioTrackRepository.findAllOrderByUploadedAtDesc()).thenReturn(expectedTracks);
        
        // Act
        List<AudioTrack> result = audioService.getAllTracks();
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(sampleTrack, result.get(0));
        verify(audioTrackRepository, times(1)).findAllOrderByUploadedAtDesc();
    }
    
    @Test
    void getTrackById_ExistingId_ShouldReturnTrack() {
        // Arrange
        when(audioTrackRepository.findById(1L)).thenReturn(Optional.of(sampleTrack));
        
        // Act
        Optional<AudioTrack> result = audioService.getTrackById(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(sampleTrack, result.get());
        verify(audioTrackRepository, times(1)).findById(1L);
    }
    
    @Test
    void getTrackById_NonExistingId_ShouldReturnEmpty() {
        // Arrange
        when(audioTrackRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        Optional<AudioTrack> result = audioService.getTrackById(999L);
        
        // Assert
        assertFalse(result.isPresent());
        verify(audioTrackRepository, times(1)).findById(999L);
    }
    
    @Test
    void getTrackByFilename_ExistingFilename_ShouldReturnTrack() {
        // Arrange
        when(audioTrackRepository.findByFilename("test-song.mp3")).thenReturn(Optional.of(sampleTrack));
        
        // Act
        Optional<AudioTrack> result = audioService.getTrackByFilename("test-song.mp3");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(sampleTrack, result.get());
        verify(audioTrackRepository, times(1)).findByFilename("test-song.mp3");
    }
    
    @Test
    void searchTracksByTitle_ShouldReturnMatchingTracks() {
        // Arrange
        List<AudioTrack> expectedTracks = Arrays.asList(sampleTrack);
        when(audioTrackRepository.findByTitleContainingIgnoreCase("Test")).thenReturn(expectedTracks);
        
        // Act
        List<AudioTrack> result = audioService.searchTracksByTitle("Test");
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(sampleTrack, result.get(0));
        verify(audioTrackRepository, times(1)).findByTitleContainingIgnoreCase("Test");
    }
    
    @Test
    void searchTracksByArtist_ShouldReturnMatchingTracks() {
        // Arrange
        List<AudioTrack> expectedTracks = Arrays.asList(sampleTrack);
        when(audioTrackRepository.findByArtistContainingIgnoreCase("Test Artist")).thenReturn(expectedTracks);
        
        // Act
        List<AudioTrack> result = audioService.searchTracksByArtist("Test Artist");
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(sampleTrack, result.get(0));
        verify(audioTrackRepository, times(1)).findByArtistContainingIgnoreCase("Test Artist");
    }
    
    @Test
    void deleteTrack_ExistingTrack_ShouldReturnTrue() {
        // Arrange
        when(audioTrackRepository.findById(1L)).thenReturn(Optional.of(sampleTrack));
        
        // Act
        boolean result = audioService.deleteTrack(1L);
        
        // Assert
        assertTrue(result);
        verify(audioTrackRepository, times(1)).findById(1L);
        verify(audioTrackRepository, times(1)).delete(sampleTrack);
    }
    
    @Test
    void deleteTrack_NonExistingTrack_ShouldReturnFalse() {
        // Arrange
        when(audioTrackRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        boolean result = audioService.deleteTrack(999L);
        
        // Assert
        assertFalse(result);
        verify(audioTrackRepository, times(1)).findById(999L);
        verify(audioTrackRepository, never()).delete(any(AudioTrack.class));
    }
    
    @Test
    void getTrackCount_ShouldReturnCount() {
        // Arrange
        when(audioTrackRepository.count()).thenReturn(5L);
        
        // Act
        long result = audioService.getTrackCount();
        
        // Assert
        assertEquals(5L, result);
        verify(audioTrackRepository, times(1)).count();
    }
    
    @Test
    void getFilePath_ShouldReturnCorrectPath() {
        // Act
        Path result = audioService.getFilePath(sampleTrack);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toString().contains("uploads"));
        assertTrue(result.toString().contains("test-song.mp3"));
    }
    
    @Test
    void fileExists_ExistingFile_ShouldReturnTrue() throws IOException {
        // Arrange - Create a temporary file in the uploads directory
        Path uploadsDir = Paths.get("test-uploads");
        Files.createDirectories(uploadsDir);
        
        Path tempFile = uploadsDir.resolve("test-song.mp3");
        Files.write(tempFile, "test content".getBytes());
        
        AudioTrack trackWithExistingFile = new AudioTrack(
            "Test", "Artist", "test-song.mp3", "test.mp3", testUser
        );
        
        // Act
        boolean result = audioService.fileExists(trackWithExistingFile);
        
        // Assert
        assertTrue(result);
        
        // Cleanup
        Files.deleteIfExists(tempFile);
        try {
            Files.deleteIfExists(uploadsDir);
        } catch (DirectoryNotEmptyException e) {
            // Directory might not be empty, try to delete all files first
            Files.walk(uploadsDir)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
        }
    }
    
    @Test
    void fileExists_NonExistingFile_ShouldReturnFalse() {
        // Arrange
        AudioTrack trackWithNonExistingFile = new AudioTrack(
            "Test", "Artist", "non-existing-file.mp3", "test.mp3", testUser
        );
        
        // Act
        boolean result = audioService.fileExists(trackWithNonExistingFile);
        
        // Assert
        assertFalse(result);
    }
}
