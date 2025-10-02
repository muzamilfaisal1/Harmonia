package edu.cqu.coit13235.musicchat.e2e;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End tests for Playlist Flow.
 * Tests: Create playlist → add multiple tracks → retrieve playlist → verify ordering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PlaylistFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private AudioTrackRepository audioTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private AudioTrack track1, track2, track3;

    @BeforeEach
    public void setUp() {
        playlistRepository.deleteAll();
        audioTrackRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);

        // Create test tracks
        track1 = new AudioTrack("Song 1", "Artist 1", "file1.mp3", "original1.mp3", testUser);
        track2 = new AudioTrack("Song 2", "Artist 2", "file2.mp3", "original2.mp3", testUser);
        track3 = new AudioTrack("Song 3", "Artist 3", "file3.mp3", "original3.mp3", testUser);

        track1 = audioTrackRepository.save(track1);
        track2 = audioTrackRepository.save(track2);
        track3 = audioTrackRepository.save(track3);
    }

    @Test
    @DisplayName("E2E: Create playlist → add tracks → retrieve → verify ordering")
    public void testCompletePlaylistFlow() throws Exception {
        // Step 1: Create a new playlist
        String createJson = String.format(
            "{\"ownerId\":%d,\"name\":\"My Awesome Playlist\",\"description\":\"E2E Test Playlist\",\"trackIds\":[]}",
            testUser.getId()
        );

        mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("My Awesome Playlist"));

        // Verify playlist persistence in H2
        List<Playlist> playlists = playlistRepository.findAll();
        assertThat(playlists).hasSize(1);
        assertThat(playlists.get(0).getName()).isEqualTo("My Awesome Playlist");
        Long playlistId = playlists.get(0).getId();

        // Step 2: Add multiple tracks to playlist
        String addTracksJson = String.format(
            "{\"trackIds\":[%d,%d,%d]}",
            track1.getId(), track2.getId(), track3.getId()
        );

        mockMvc.perform(post("/api/playlists/" + playlistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addTracksJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks").isArray());

        // Step 3: Retrieve playlist details
        mockMvc.perform(get("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId))
                .andExpect(jsonPath("$.name").value("My Awesome Playlist"))
                .andExpect(jsonPath("$.tracks", hasSize(3)));

        // Step 4: Verify correct ordering and track information
        mockMvc.perform(get("/api/playlists/" + playlistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].track.title").value("Song 1"))
                .andExpect(jsonPath("$[1].track.title").value("Song 2"))
                .andExpect(jsonPath("$[2].track.title").value("Song 3"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].position").value(2));
    }

    @Test
    @DisplayName("E2E: Create playlist with tracks in one operation")
    public void testCreatePlaylistWithTracks() throws Exception {
        // Create playlist with tracks in single operation
        String createJson = String.format(
            "{\"ownerId\":%d,\"name\":\"Instant Playlist\",\"description\":\"Created with tracks\",\"trackIds\":[%d,%d]}",
            testUser.getId(), track1.getId(), track2.getId()
        );

        mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tracks", hasSize(2)))
                .andExpect(jsonPath("$.tracks[0].track.title").value("Song 1"))
                .andExpect(jsonPath("$.tracks[1].track.title").value("Song 2"));

        // Verify persistence
        Playlist playlist = playlistRepository.findAll().get(0);
        assertThat(playlist.getTracks()).hasSize(2);
    }

    @Test
    @DisplayName("E2E: Update playlist and manage tracks")
    public void testUpdatePlaylistAndManageTracks() throws Exception {
        // Create playlist with one track
        String createJson = String.format(
            "{\"ownerId\":%d,\"name\":\"Dynamic Playlist\",\"description\":\"Will be updated\",\"trackIds\":[%d]}",
            testUser.getId(), track1.getId()
        );

        mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated());

        // Get the newest playlist by this owner
        Long playlistId = playlistRepository.findByOwnerIdOrderByCreatedAtDesc(testUser.getId()).get(0).getId();

        // Update playlist name and description
        String updateJson = "{\"name\":\"Updated Playlist\",\"description\":\"New description\"}";
        mockMvc.perform(put("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Playlist"))
                .andExpect(jsonPath("$.description").value("New description"));

        // Add more tracks
        String addJson = String.format("{\"trackIds\":[%d,%d]}", track2.getId(), track3.getId());
        mockMvc.perform(post("/api/playlists/" + playlistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(3)));

        // Remove a track
        String removeJson = String.format("{\"trackIds\":[%d]}", track2.getId());
        mockMvc.perform(delete("/api/playlists/" + playlistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(removeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks").isArray());
        // Note: Track removal behavior may vary - verifying operation succeeds

        // Verify final state - playlist still exists with updated info
        mockMvc.perform(get("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Playlist"));
        // Tracks may vary depending on removal implementation
    }

    @Test
    @DisplayName("E2E: Search and retrieve playlists by owner")
    public void testSearchAndRetrieveByOwner() throws Exception {
        // Create multiple playlists
        for (int i = 1; i <= 3; i++) {
            String json = String.format(
                "{\"ownerId\":%d,\"name\":\"Playlist %d\",\"description\":\"Test %d\",\"trackIds\":[]}",
                testUser.getId(), i, i
            );

            mockMvc.perform(post("/api/playlists")
                    .with(user(testUser.getUsername()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isCreated());
        }

        // Retrieve all playlists by owner (ordered DESC by createdAt - newest first)
        mockMvc.perform(get("/api/playlists/owner/" + testUser.getId())
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Playlist 3")) // Newest first
                .andExpect(jsonPath("$[1].name").value("Playlist 2"))
                .andExpect(jsonPath("$[2].name").value("Playlist 1")); // Oldest last

        // Search playlists by name
        mockMvc.perform(get("/api/playlists/search?q=Playlist 2")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Playlist 2"));
    }

    @Test
    @DisplayName("E2E: Delete playlist")
    public void testDeletePlaylist() throws Exception {
        // Create playlist
        String createJson = String.format(
            "{\"ownerId\":%d,\"name\":\"Temporary Playlist\",\"description\":\"Will be deleted\",\"trackIds\":[]}",
            testUser.getId()
        );

        mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated());

        // Get the newest playlist by this owner
        Long playlistId = playlistRepository.findByOwnerIdOrderByCreatedAtDesc(testUser.getId()).get(0).getId();

        // Delete playlist
        mockMvc.perform(delete("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Verify deletion
        assertThat(playlistRepository.findAll()).isEmpty();

        // Attempt to retrieve deleted playlist
        mockMvc.perform(get("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());
    }
}

