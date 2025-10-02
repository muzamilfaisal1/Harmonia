package edu.cqu.coit13235.musicchat.e2e;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import edu.cqu.coit13235.musicchat.service.ExternalMusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End tests for Audio Flow.
 * Tests: Upload audio → fetch metadata → add to playlist → verify playlist contents.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AudioFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AudioTrackRepository audioTrackRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ExternalMusicService externalMusicService;

    private User testUser;

    @BeforeEach
    public void setUp() {
        audioTrackRepository.deleteAll();
        playlistRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);

        // Mock external music service
        ExternalMusicService.MusicMetadata metadata = new ExternalMusicService.MusicMetadata();
        metadata.setTitle("Test Song");
        metadata.setArtist("Test Artist");
        metadata.setAlbum("Test Album");
        metadata.setDuration(180);
        
        when(externalMusicService.searchMusic(anyString())).thenReturn(List.of(metadata));
    }

    @Test
    @DisplayName("E2E: Upload audio → verify persistence → add to playlist → confirm playlist")
    public void testCompleteAudioFlow() throws Exception {
        // Step 1: Upload an audio file
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "test-song.mp3",
            "audio/mpeg",
            "test audio content".getBytes()
        );

        String uploadResponse = mockMvc.perform(multipart("/api/audio/upload")
                .file(audioFile)
                .param("title", "Test Song")
                .param("artist", "Test Artist")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Song"))
                .andExpect(jsonPath("$.artist").value("Test Artist"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 2: Verify persistence in H2
        List<AudioTrack> tracks = audioTrackRepository.findAll();
        assertThat(tracks).hasSize(1);
        assertThat(tracks.get(0).getTitle()).isEqualTo("Test Song");
        assertThat(tracks.get(0).getArtist()).isEqualTo("Test Artist");
        Long trackId = tracks.get(0).getId();

        // Step 3: Fetch metadata from external service (mocked)
        mockMvc.perform(get("/api/audio/search/external?query=Test Song")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].title").value("Test Song"));

        // Step 4: Create a playlist
        String playlistJson = String.format(
            "{\"ownerId\":%d,\"name\":\"My Test Playlist\",\"description\":\"E2E Test\",\"trackIds\":[]}",
            testUser.getId()
        );

        String playlistResponse = mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(playlistJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long playlistId = playlistRepository.findAll().get(0).getId();

        // Step 5: Add uploaded track to playlist
        String addTrackJson = String.format("{\"trackIds\":[%d]}", trackId);
        mockMvc.perform(post("/api/playlists/" + playlistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addTrackJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks").isArray());

        // Step 6: Verify playlist contains the track with correct metadata
        mockMvc.perform(get("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(1)))
                .andExpect(jsonPath("$.tracks[0].track.title").value("Test Song"))
                .andExpect(jsonPath("$.tracks[0].track.artist").value("Test Artist"));
    }

    @Test
    @DisplayName("E2E: Upload multiple tracks and add to playlist in correct order")
    public void testMultipleTracksInPlaylist() throws Exception {
        // Upload 3 tracks
        String[] titles = {"Song A", "Song B", "Song C"};
        Long[] trackIds = new Long[3];

        for (int i = 0; i < titles.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                titles[i] + ".mp3",
                "audio/mpeg",
                ("content " + i).getBytes()
            );

            mockMvc.perform(multipart("/api/audio/upload")
                    .file(file)
                    .param("title", titles[i])
                    .param("artist", "Artist " + i)
                    .with(user(testUser.getUsername()).roles("USER")))
                    .andExpect(status().isCreated());

            trackIds[i] = audioTrackRepository.findAll().get(i).getId();
        }

        // Verify all tracks are stored
        assertThat(audioTrackRepository.findAll()).hasSize(3);

        // Create playlist with all tracks
        String playlistJson = String.format(
            "{\"ownerId\":%d,\"name\":\"Multi-Track Playlist\",\"description\":\"Test\",\"trackIds\":[%d,%d,%d]}",
            testUser.getId(), trackIds[0], trackIds[1], trackIds[2]
        );

        mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(playlistJson))
                .andExpect(status().isCreated());

        Long playlistId = playlistRepository.findAll().get(0).getId();

        // Verify playlist contains all tracks in correct order
        mockMvc.perform(get("/api/playlists/" + playlistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(3)))
                .andExpect(jsonPath("$.tracks[0].track.title").value("Song A"))
                .andExpect(jsonPath("$.tracks[1].track.title").value("Song B"))
                .andExpect(jsonPath("$.tracks[2].track.title").value("Song C"));
    }

    @Test
    @DisplayName("E2E: Retrieve and download uploaded audio")
    public void testRetrieveAndDownloadAudio() throws Exception {
        // Upload audio
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "download-test.mp3",
            "audio/mpeg",
            "downloadable content".getBytes()
        );

        mockMvc.perform(multipart("/api/audio/upload")
                .file(audioFile)
                .param("title", "Download Test")
                .param("artist", "Test Artist")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isCreated());

        Long trackId = audioTrackRepository.findAll().get(0).getId();

        // Retrieve track details
        mockMvc.perform(get("/api/audio/tracks/" + trackId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Download Test"));

        // Download track (file should exist)
        mockMvc.perform(get("/api/audio/download/" + trackId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk());
    }
}

