package edu.cqu.coit13235.musicchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.PlaylistTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PlaylistController.
 * Tests playlist REST endpoints with real database interactions.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PlaylistControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private PlaylistRepository playlistRepository;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private AudioTrack testTrack1;
    private AudioTrack testTrack2;
    private Playlist testPlaylist;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        // Create test user with unique email
        testUser = new User("playlistuser", "playlistuser@example.com", "password");
        testUser = userRepository.save(testUser);
        
        // Create test audio tracks
        testTrack1 = new AudioTrack("Song 1", "Artist 1", "song1.mp3", "song1.mp3", testUser);
        testTrack1.setUploadedAt(LocalDateTime.now());
        testTrack1 = audioTrackRepository.save(testTrack1);
        
        testTrack2 = new AudioTrack("Song 2", "Artist 2", "song2.mp3", "song2.mp3", testUser);
        testTrack2.setUploadedAt(LocalDateTime.now());
        testTrack2 = audioTrackRepository.save(testTrack2);
        
        // Create test playlist
        testPlaylist = new Playlist(1L, "Test Playlist", "A test playlist");
        testPlaylist.setCreatedAt(LocalDateTime.now());
        testPlaylist = playlistRepository.save(testPlaylist);
    }
    
    @Test
    void createPlaylist_ValidData_ShouldReturnCreatedPlaylist() throws Exception {
        // Arrange
        System.out.println("🧪 [TEST DEBUG] createPlaylist_ValidData_ShouldReturnCreatedPlaylist - Starting test");
        System.out.println("🧪 [TEST DEBUG] testTrack1.getId() = " + testTrack1.getId());
        System.out.println("🧪 [TEST DEBUG] testTrack2.getId() = " + testTrack2.getId());
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ownerId", 1L);
        requestData.put("name", "New Playlist");
        requestData.put("description", "A new test playlist");
        requestData.put("trackIds", Arrays.asList(testTrack1.getId(), testTrack2.getId()));
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        System.out.println("🧪 [TEST DEBUG] Request data: " + requestData);
        System.out.println("🧪 [TEST DEBUG] Request body: " + requestBody);
        
        // Act & Assert
        System.out.println("🧪 [TEST DEBUG] Making POST request to /api/playlists");
        
        mockMvc.perform(post("/api/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(result -> {
                    System.out.println("🧪 [TEST DEBUG] Response status: " + result.getResponse().getStatus());
                    System.out.println("🧪 [TEST DEBUG] Response content: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Playlist")))
                .andExpect(jsonPath("$.description", is("A new test playlist")))
                .andExpect(jsonPath("$.ownerId", is(1)))
                .andExpect(jsonPath("$.id", notNullValue()));
        
        System.out.println("🧪 [TEST DEBUG] Test completed successfully");
    }
    
    @Test
    void createPlaylist_EmptyName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ownerId", 1L);
        requestData.put("name", "");
        requestData.put("description", "A test playlist");
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        // Act & Assert
        mockMvc.perform(post("/api/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid input")))
                .andExpect(jsonPath("$.message", is("Playlist name is required")));
    }
    
    @Test
    void createPlaylist_MissingName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ownerId", 1L);
        requestData.put("description", "A test playlist");
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        // Act & Assert
        mockMvc.perform(post("/api/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid input")))
                .andExpect(jsonPath("$.message", is("Playlist name is required")));
    }
    
    @Test
    void getPlaylistById_ExistingId_ShouldReturnPlaylist() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/{id}", testPlaylist.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPlaylist.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Playlist")))
                .andExpect(jsonPath("$.description", is("A test playlist")))
                .andExpect(jsonPath("$.ownerId", is(1)));
    }
    
    @Test
    void getPlaylistById_NonExistingId_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/{id}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getPlaylistByIdAndOwner_ValidIds_ShouldReturnPlaylist() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/{id}/owner/{ownerId}", testPlaylist.getId(), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPlaylist.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Playlist")))
                .andExpect(jsonPath("$.ownerId", is(1)));
    }
    
    @Test
    void getPlaylistByIdAndOwner_InvalidOwner_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/{id}/owner/{ownerId}", testPlaylist.getId(), 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getPlaylistsByOwner_ValidOwner_ShouldReturnPlaylists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/owner/{ownerId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].ownerId", is(1)));
    }
    
    @Test
    void getAllPlaylists_ShouldReturnAllPlaylists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }
    
    @Test
    void searchPlaylistsByName_ValidQuery_ShouldReturnMatchingPlaylists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/search")
                .param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", containsString("Test")));
    }
    
    @Test
    void searchPlaylistsByName_EmptyQuery_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/search")
                .param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid query")))
                .andExpect(jsonPath("$.message", is("Search query cannot be empty")));
    }
    
    @Test
    void getPlaylistTracks_ExistingPlaylist_ShouldReturnTracks() throws Exception {
        // Arrange - Add tracks to playlist
        PlaylistTrack playlistTrack1 = new PlaylistTrack();
        playlistTrack1.setPlaylist(testPlaylist);
        playlistTrack1.setTrack(testTrack1);
        playlistTrack1.setPosition(0);
        
        PlaylistTrack playlistTrack2 = new PlaylistTrack();
        playlistTrack2.setPlaylist(testPlaylist);
        playlistTrack2.setTrack(testTrack2);
        playlistTrack2.setPosition(1);
        
        playlistTrackRepository.saveAll(Arrays.asList(playlistTrack1, playlistTrack2));
        
        // Act & Assert
        mockMvc.perform(get("/api/playlists/{id}/tracks", testPlaylist.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].position", is(0)))
                .andExpect(jsonPath("$[1].position", is(1)));
    }
    
    @Test
    void addTracksToPlaylist_ValidData_ShouldReturnUpdatedPlaylist() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("trackIds", Arrays.asList(testTrack1.getId(), testTrack2.getId()));
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        // Act & Assert
        mockMvc.perform(post("/api/playlists/{id}/tracks", testPlaylist.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPlaylist.getId().intValue())));
    }
    
    @Test
    void addTracksToPlaylist_EmptyTrackIds_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("trackIds", Arrays.asList());
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        // Act & Assert
        mockMvc.perform(post("/api/playlists/{id}/tracks", testPlaylist.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid input")))
                .andExpect(jsonPath("$.message", is("trackIds is required and cannot be empty")));
    }
    
    @Test
    void removeTracksFromPlaylist_ValidData_ShouldReturnUpdatedPlaylist() throws Exception {
        // Arrange - Add tracks first
        System.out.println("🧪 [TEST DEBUG] removeTracksFromPlaylist_ValidData_ShouldReturnUpdatedPlaylist - Starting test");
        System.out.println("🧪 [TEST DEBUG] testTrack1.getId() = " + testTrack1.getId());
        System.out.println("🧪 [TEST DEBUG] testTrack2.getId() = " + testTrack2.getId());
        System.out.println("🧪 [TEST DEBUG] testPlaylist.getId() = " + testPlaylist.getId());
        
        PlaylistTrack playlistTrack1 = new PlaylistTrack();
        playlistTrack1.setPlaylist(testPlaylist);
        playlistTrack1.setTrack(testTrack1);
        playlistTrack1.setPosition(0);
        
        PlaylistTrack playlistTrack2 = new PlaylistTrack();
        playlistTrack2.setPlaylist(testPlaylist);
        playlistTrack2.setTrack(testTrack2);
        playlistTrack2.setPosition(1);
        
        System.out.println("🧪 [TEST DEBUG] Saving PlaylistTrack entries to database");
        playlistTrackRepository.saveAll(Arrays.asList(playlistTrack1, playlistTrack2));
        System.out.println("🧪 [TEST DEBUG] PlaylistTrack entries saved successfully");
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("trackIds", Arrays.asList(testTrack1.getId()));
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        System.out.println("🧪 [TEST DEBUG] Request data: " + requestData);
        System.out.println("🧪 [TEST DEBUG] Request body: " + requestBody);
        
        // Act & Assert
        System.out.println("🧪 [TEST DEBUG] Making DELETE request to /api/playlists/" + testPlaylist.getId() + "/tracks");
        
        mockMvc.perform(delete("/api/playlists/{id}/tracks", testPlaylist.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(result -> {
                    System.out.println("🧪 [TEST DEBUG] Response status: " + result.getResponse().getStatus());
                    System.out.println("🧪 [TEST DEBUG] Response content: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPlaylist.getId().intValue())));
        
        System.out.println("🧪 [TEST DEBUG] Test completed successfully");
    }
    
    @Test
    void updatePlaylist_ValidData_ShouldReturnUpdatedPlaylist() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("name", "Updated Playlist");
        requestData.put("description", "Updated description");
        String requestBody = objectMapper.writeValueAsString(requestData);
        
        // Act & Assert
        mockMvc.perform(put("/api/playlists/{id}", testPlaylist.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Playlist")))
                .andExpect(jsonPath("$.description", is("Updated description")));
    }
    
    @Test
    void deletePlaylist_ExistingPlaylist_ShouldReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/playlists/{id}", testPlaylist.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Playlist deleted successfully")));
    }
    
    @Test
    void deletePlaylist_NonExistingPlaylist_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/playlists/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Playlist not found")));
    }
    
    @Test
    void getPlaylistCount_ShouldReturnCount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }
    
    @Test
    void getPlaylistCountByOwner_ValidOwner_ShouldReturnCount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/playlists/count/owner/{ownerId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }
}
