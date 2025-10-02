package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AudioController.
 * Tests audio upload and retrieval functionality with real database interactions.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AudioControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private MockMvc mockMvc;
    
    private MockMultipartFile validAudioFile;
    private MockMultipartFile invalidFile;
    private AudioTrack testTrack;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create test user
        testUser = new User("testuser", "test@example.com", "password");
        testUser = userRepository.save(testUser);
        
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
        
        // Create a test track in database
        testTrack = new AudioTrack("Test Song", "Test Artist", "test-song.mp3", "test-song.mp3", testUser);
        testTrack.setUploadedAt(LocalDateTime.now());
        testTrack.setFileSizeBytes(1024L);
        testTrack.setContentType("audio/mpeg");
        testTrack = audioTrackRepository.save(testTrack);
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void uploadAudio_ValidFile_ShouldReturnCreatedAudioTrack() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/audio/upload")
                .file(validAudioFile)
                .param("title", "New Song")
                .param("artist", "New Artist"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("New Song")))
                .andExpect(jsonPath("$.artist", is("New Artist")))
                .andExpect(jsonPath("$.contentType", is("audio/mpeg")))
                .andExpect(jsonPath("$.filename", notNullValue()))
                .andExpect(jsonPath("$.id", notNullValue()));
        
        // Verify track was saved to database
        List<AudioTrack> tracks = audioTrackRepository.findAll();
        assertTrue(tracks.size() >= 2); // At least the test track and the new one
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void uploadAudio_EmptyTitle_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/audio/upload")
                .file(validAudioFile)
                .param("title", "")
                .param("artist", "New Artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Title is required")))
                .andExpect(jsonPath("$.message", is("Title cannot be null or empty")));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void uploadAudio_EmptyArtist_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/audio/upload")
                .file(validAudioFile)
                .param("title", "New Song")
                .param("artist", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Artist is required")))
                .andExpect(jsonPath("$.message", is("Artist cannot be null or empty")));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void uploadAudio_InvalidFileType_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/audio/upload")
                .file(invalidFile)
                .param("title", "New Song")
                .param("artist", "New Artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid input")))
                .andExpect(jsonPath("$.message", containsString("Invalid file type")));
    }
    
    @Test
    void getAllTracks_ShouldReturnAllTracks() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].title", notNullValue()))
                .andExpect(jsonPath("$[0].artist", notNullValue()));
    }
    
    @Test
    void getTrackById_ExistingId_ShouldReturnTrack() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/tracks/{id}", testTrack.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testTrack.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Test Song")))
                .andExpect(jsonPath("$.artist", is("Test Artist")));
    }
    
    @Test
    void getTrackById_NonExistingId_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/tracks/{id}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void searchTracksByTitle_ValidQuery_ShouldReturnMatchingTracks() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/search/title")
                .param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title", containsString("Test")));
    }
    
    @Test
    void searchTracksByTitle_EmptyQuery_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/search/title")
                .param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid query")))
                .andExpect(jsonPath("$.message", is("Search query cannot be empty")));
    }
    
    @Test
    void searchTracksByArtist_ValidQuery_ShouldReturnMatchingTracks() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/search/artist")
                .param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].artist", containsString("Test")));
    }
    
    @Test
    void searchTracksByArtist_EmptyQuery_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/search/artist")
                .param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid query")))
                .andExpect(jsonPath("$.message", is("Search query cannot be empty")));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void deleteTrack_ExistingTrack_ShouldReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/audio/tracks/{id}", testTrack.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Track deleted successfully")));
        
        // Verify track was deleted from database
        assertFalse(audioTrackRepository.findById(testTrack.getId()).isPresent());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void deleteTrack_NonExistingTrack_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/audio/tracks/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Track not found")));
    }
    
    @Test
    void getTrackCount_ShouldReturnCount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }
    
    @Test
    void downloadTrack_ExistingTrackButNoFile_ShouldReturnNotFound() throws Exception {
        // Note: This test verifies that when a track exists in DB but file doesn't exist on disk,
        // the system returns 404 (which is the correct behavior)
        mockMvc.perform(get("/api/audio/download/{id}", testTrack.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("File not found")))
                .andExpect(jsonPath("$.message", is("Audio file does not exist on disk")));
    }
    
    @Test
    void downloadTrack_WithActualFile_ShouldReturnFile() throws Exception {
        // Create a temporary file for testing
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-audio", ".mp3");
        java.nio.file.Files.write(tempFile, "test audio content".getBytes());
        
        // Create a track that points to this file
        AudioTrack trackWithFile = new AudioTrack("Test File", "Test Artist", tempFile.getFileName().toString(), "test-file.mp3", testUser);
        trackWithFile.setUploadedAt(LocalDateTime.now());
        trackWithFile.setFileSizeBytes(1024L);
        trackWithFile.setContentType("audio/mpeg");
        trackWithFile = audioTrackRepository.save(trackWithFile);
        
        try {
            // Move the file to the uploads directory
            java.nio.file.Path uploadsDir = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadsDir)) {
                java.nio.file.Files.createDirectories(uploadsDir);
            }
            java.nio.file.Path targetPath = uploadsDir.resolve(tempFile.getFileName());
            java.nio.file.Files.move(tempFile, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Test download
            mockMvc.perform(get("/api/audio/download/{id}", trackWithFile.getId()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", 
                        containsString("attachment; filename=\"" + trackWithFile.getOriginalFilename() + "\"")));
        } finally {
            // Cleanup
            java.nio.file.Files.deleteIfExists(tempFile);
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("uploads", tempFile.getFileName().toString()));
        }
    }
    
    @Test
    void downloadTrack_NonExistingTrack_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/audio/download/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
