package edu.cqu.coit13235.musicchat.controller;

import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;

/**
 * Tests that simulate uploading audio files and creating playlists.
 * These tests validate the complete upload and playlist creation workflow
 * as required for Person B (Nirob) Week 9 deliverables.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AudioUploadSimulationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create test users if they don't exist
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User("testuser", "test@example.com", "password", "USER");
            userRepository.save(testUser);
        }
        
        if (!userRepository.existsByUsername("user1")) {
            User user1 = new User("user1", "user1@example.com", "password", "USER");
            userRepository.save(user1);
        }
        
        if (!userRepository.existsByUsername("user2")) {
            User user2 = new User("user2", "user2@example.com", "password", "USER");
            userRepository.save(user2);
        }
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulateAudioUpload() throws Exception {
        // Simulate uploading an audio file
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "test-song.mp3",
            "audio/mpeg",
            "fake audio content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/audio/upload")
                .file(audioFile)
                .param("title", "Test Song")
                .param("artist", "Test Artist"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Song"))
                .andExpect(jsonPath("$.artist").value("Test Artist"))
                .andExpect(jsonPath("$.filename").exists())
                .andExpect(jsonPath("$.uploadedAt").exists());
        
        // Verify the track was saved in the database
        List<AudioTrack> tracks = audioTrackRepository.findByTitleContainingIgnoreCase("Test Song");
        assert !tracks.isEmpty();
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulateMultipleUploads() throws Exception {
        // Simulate uploading multiple audio files
        String[] songs = {"Song 1", "Song 2", "Song 3"};
        String[] artists = {"Artist 1", "Artist 2", "Artist 3"};
        
        for (int i = 0; i < songs.length; i++) {
            MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "song" + (i + 1) + ".mp3",
                "audio/mpeg",
                ("fake audio content " + (i + 1)).getBytes()
            );
            
            mockMvc.perform(multipart("/api/audio/upload")
                    .file(audioFile)
                    .param("title", songs[i])
                    .param("artist", artists[i]))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value(songs[i]))
                    .andExpect(jsonPath("$.artist").value(artists[i]));
        }
        
        // Verify all tracks were saved
        assert !audioTrackRepository.findByTitleContainingIgnoreCase("Song 1").isEmpty();
        assert !audioTrackRepository.findByTitleContainingIgnoreCase("Song 2").isEmpty();
        assert !audioTrackRepository.findByTitleContainingIgnoreCase("Song 3").isEmpty();
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulatePlaylistCreation() throws Exception {
        // First, create some audio tracks
        createTestTracks();
        
        // Get the track IDs
        List<AudioTrack> track1List = audioTrackRepository.findByTitleContainingIgnoreCase("Playlist Song 1");
        List<AudioTrack> track2List = audioTrackRepository.findByTitleContainingIgnoreCase("Playlist Song 2");
        assert !track1List.isEmpty() && !track2List.isEmpty();
        AudioTrack track1 = track1List.get(0);
        AudioTrack track2 = track2List.get(0);
        
        // Simulate creating a playlist with these tracks
        String playlistJson = String.format(
            "{\"ownerId\":1,\"name\":\"Test Playlist\",\"description\":\"A test playlist\",\"trackIds\":[%d,%d]}",
            track1.getId(), track2.getId()
        );
        
        mockMvc.perform(post("/api/playlists")
                .contentType("application/json")
                .content(playlistJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Playlist"))
                .andExpect(jsonPath("$.description").value("A test playlist"))
                .andExpect(jsonPath("$.ownerId").value(1));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulatePlaylistCreationAndRetrieval() throws Exception {
        // Create test tracks
        createTestTracks();
        
        // Create playlist
        List<AudioTrack> track1List = audioTrackRepository.findByTitleContainingIgnoreCase("Playlist Song 1");
        List<AudioTrack> track2List = audioTrackRepository.findByTitleContainingIgnoreCase("Playlist Song 2");
        assert !track1List.isEmpty() && !track2List.isEmpty();
        AudioTrack track1 = track1List.get(0);
        AudioTrack track2 = track2List.get(0);
        
        String playlistJson = String.format(
            "{\"ownerId\":1,\"name\":\"Retrieval Test Playlist\",\"description\":\"Testing retrieval\",\"trackIds\":[%d,%d]}",
            track1.getId(), track2.getId()
        );
        
        var result = mockMvc.perform(post("/api/playlists")
                .contentType("application/json")
                .content(playlistJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Extract playlist ID from response
        String responseContent = result.getResponse().getContentAsString();
        // Simple extraction - in real scenario would use JSON parsing
        Long playlistId = extractPlaylistId(responseContent);
        
        // Retrieve the playlist
        mockMvc.perform(get("/api/playlists/" + playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Retrieval Test Playlist"))
                .andExpect(jsonPath("$.description").value("Testing retrieval"));
        
        // Retrieve playlist tracks
        mockMvc.perform(get("/api/playlists/" + playlistId + "/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].track.title").value(anyOf(equalTo("Playlist Song 1"), equalTo("Playlist Song 2"))))
                .andExpect(jsonPath("$[1].track.title").value(anyOf(equalTo("Playlist Song 1"), equalTo("Playlist Song 2"))));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulateUploadValidation() throws Exception {
        // Test invalid file type
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "not an audio file".getBytes()
        );
        
        mockMvc.perform(multipart("/api/audio/upload")
                .file(invalidFile)
                .param("title", "Invalid Song")
                .param("artist", "Invalid Artist"))
                .andExpect(status().isBadRequest());
        
        // Test missing required fields
        MockMultipartFile validFile = new MockMultipartFile(
            "file",
            "test.mp3",
            "audio/mpeg",
            "fake audio content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/audio/upload")
                .file(validFile)
                .param("title", "") // Empty title
                .param("artist", "Test Artist"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulateSearchFunctionality() throws Exception {
        // Create test tracks with specific titles and artists
        createTestTracks();
        
        // Test search by title
        mockMvc.perform(get("/api/audio/search/title")
                .param("q", "Playlist Song"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        
        // Test search by artist
        mockMvc.perform(get("/api/audio/search/artist")
                .param("q", "Playlist Artist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        
        // Test search with no results
        mockMvc.perform(get("/api/audio/search/title")
                .param("q", "Nonexistent Song"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void testSimulatePlaylistSearch() throws Exception {
        // Create a playlist
        createTestTracks();
        List<AudioTrack> trackList = audioTrackRepository.findByTitleContainingIgnoreCase("Playlist Song 1");
        assert !trackList.isEmpty();
        AudioTrack track = trackList.get(0);
        
        String playlistJson = String.format(
            "{\"ownerId\":1,\"name\":\"Searchable Playlist\",\"description\":\"A playlist for search testing\",\"trackIds\":[%d]}",
            track.getId()
        );
        
        mockMvc.perform(post("/api/playlists")
                .contentType("application/json")
                .content(playlistJson))
                .andExpect(status().isCreated());
        
        // Search for the playlist
        mockMvc.perform(get("/api/playlists/search")
                .param("q", "Searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name").value(containsString("Searchable")));
    }
    
    /**
     * Helper method to create test tracks for playlist testing
     */
    private void createTestTracks() throws Exception {
        String[] songs = {"Playlist Song 1", "Playlist Song 2"};
        String[] artists = {"Playlist Artist 1", "Playlist Artist 2"};
        
        for (int i = 0; i < songs.length; i++) {
            MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "playlist-song" + (i + 1) + ".mp3",
                "audio/mpeg",
                ("fake audio content " + (i + 1)).getBytes()
            );
            
            mockMvc.perform(multipart("/api/audio/upload")
                    .file(audioFile)
                    .param("title", songs[i])
                    .param("artist", artists[i]))
                    .andExpect(status().isCreated());
        }
    }
    
    /**
     * Helper method to extract playlist ID from JSON response
     * This is a simplified implementation for testing purposes
     */
    private Long extractPlaylistId(String jsonResponse) {
        // Simple regex extraction - in production would use proper JSON parsing
        try {
            String idPattern = "\"id\":(\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return Long.valueOf(matcher.group(1));
            }
        } catch (NumberFormatException | IllegalStateException e) {
            // Fallback to a default ID for testing
            return 1L;
        }
        return 1L;
    }
}
