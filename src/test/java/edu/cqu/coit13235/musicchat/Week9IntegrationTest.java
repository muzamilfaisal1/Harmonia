package edu.cqu.coit13235.musicchat;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.PlaylistTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import edu.cqu.coit13235.musicchat.service.AudioService;
import edu.cqu.coit13235.musicchat.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify Week 9 functionality.
 * Tests the complete audio upload and playlist creation workflow.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Week9IntegrationTest {
    
    @Autowired
    private AudioService audioService;
    
    @Autowired
    private PlaylistService playlistService;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    @Autowired
    private PlaylistRepository playlistRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testAudioTrackCreation() {
        // Create test user with unique email
        User testUser = new User("week9user1", "week9user1@example.com", "password");
        testUser = userRepository.save(testUser);
        
        // Test creating an audio track
        AudioTrack track = new AudioTrack("Test Song", "Test Artist", "test.mp3", "test.mp3", testUser);
        track.setUploadedAt(LocalDateTime.now());
        track.setFileSizeBytes(1024L);
        track.setContentType("audio/mpeg");
        
        AudioTrack savedTrack = audioTrackRepository.save(track);
        
        assertNotNull(savedTrack.getId());
        assertEquals("Test Song", savedTrack.getTitle());
        assertEquals("Test Artist", savedTrack.getArtist());
        assertEquals("audio/mpeg", savedTrack.getContentType());
    }
    
    @Test
    void testPlaylistCreation() {
        // Test creating a playlist
        Playlist playlist = new Playlist(1L, "Test Playlist", "A test playlist");
        playlist.setCreatedAt(LocalDateTime.now());
        
        Playlist savedPlaylist = playlistRepository.save(playlist);
        
        assertNotNull(savedPlaylist.getId());
        assertEquals("Test Playlist", savedPlaylist.getName());
        assertEquals(1L, savedPlaylist.getOwnerId());
        assertEquals("A test playlist", savedPlaylist.getDescription());
    }
    
    @Test
    void testPlaylistWithTracks() {
        // Create test user with unique email
        User testUser = new User("week9user2", "week9user2@example.com", "password");
        testUser = userRepository.save(testUser);
        
        // Create audio tracks
        AudioTrack track1 = new AudioTrack("Song 1", "Artist 1", "song1.mp3", "song1.mp3", testUser);
        track1.setUploadedAt(LocalDateTime.now());
        track1 = audioTrackRepository.save(track1);
        
        AudioTrack track2 = new AudioTrack("Song 2", "Artist 2", "song2.mp3", "song2.mp3", testUser);
        track2.setUploadedAt(LocalDateTime.now());
        track2 = audioTrackRepository.save(track2);
        
        // Create playlist with tracks
        Playlist playlist = playlistService.createPlaylistWithTracks(
            1L, 
            "My Playlist", 
            "A playlist with tracks",
            Arrays.asList(track1.getId(), track2.getId())
        );
        
        assertNotNull(playlist.getId());
        assertEquals("My Playlist", playlist.getName());
        
        // Check track count using the service method
        List<PlaylistTrack> playlistTracks = playlistService.getPlaylistTracks(playlist.getId());
        assertEquals(2, playlistTracks.size());
    }
    
    @Test
    void testAudioServiceFunctionality() {
        // Test getting all tracks
        List<AudioTrack> tracks = audioService.getAllTracks();
        assertNotNull(tracks);
        
        // Test track count
        long count = audioService.getTrackCount();
        assertTrue(count >= 0);
    }
    
    @Test
    void testPlaylistServiceFunctionality() {
        // Test getting all playlists
        List<Playlist> playlists = playlistService.getAllPlaylists();
        assertNotNull(playlists);
        
        // Test playlist count
        long count = playlistService.getPlaylistCount();
        assertTrue(count >= 0);
    }
}
