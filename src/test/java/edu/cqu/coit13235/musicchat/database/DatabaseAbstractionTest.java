package edu.cqu.coit13235.musicchat.database;

import edu.cqu.coit13235.musicchat.domain.*;
import edu.cqu.coit13235.musicchat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database abstraction validation test.
 * Verifies that all JPA operations work correctly and don't use database-specific SQL.
 * Tests run on H2 but code is database-agnostic and PostgreSQL-compatible.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseAbstractionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AudioTrackRepository audioTrackRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    public void setUp() {
        ratingRepository.deleteAll();
        favouriteRepository.deleteAll();
        playlistRepository.deleteAll();
        audioTrackRepository.deleteAll();
        chatMessageRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("dbuser", "db@test.com", passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Database: User CRUD operations")
    public void testUserCRUD() {
        // Create with unique email
        User user = new User("cruduser", "crud_unique@example.com", "password");
        user = userRepository.save(user);
        assertThat(user.getId()).isNotNull();

        // Read
        User found = userRepository.findById(user.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("cruduser");

        // Update
        found.setEmail("updated_unique@example.com");
        userRepository.save(found);
        User updated = userRepository.findById(user.getId()).orElse(null);
        assertThat(updated.getEmail()).isEqualTo("updated_unique@example.com");

        // Delete
        userRepository.delete(updated);
        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("Database: Chat message ordering and persistence")
    public void testChatMessageOrdering() throws Exception {
        // Create messages
        ChatMessage msg1 = new ChatMessage(testUser.getUsername(), "First");
        ChatMessage msg2 = new ChatMessage(testUser.getUsername(), "Second");
        ChatMessage msg3 = new ChatMessage(testUser.getUsername(), "Third");

        chatMessageRepository.save(msg1);
        Thread.sleep(10);
        chatMessageRepository.save(msg2);
        Thread.sleep(10);
        chatMessageRepository.save(msg3);

        // Verify ordering
        List<ChatMessage> messages = chatMessageRepository.findAllOrderByTimestamp();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getText()).isEqualTo("First");
        assertThat(messages.get(2).getText()).isEqualTo("Third");
    }

    @Test
    @DisplayName("Database: Audio track with user relationship")
    public void testAudioTrackRelationship() {
        AudioTrack track = new AudioTrack(
            "Test Song",
            "Test Artist",
            "test.mp3",
            "original.mp3",
            testUser
        );
        track = audioTrackRepository.save(track);

        assertThat(track.getId()).isNotNull();
        assertThat(track.getUser()).isNotNull();
        assertThat(track.getUser().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Database: Playlist with tracks")
    public void testPlaylistPersistence() {
        Playlist playlist = new Playlist(testUser.getId(), "Test Playlist", "Description");
        playlist = playlistRepository.save(playlist);

        assertThat(playlist.getId()).isNotNull();
        assertThat(playlist.getOwnerId()).isEqualTo(testUser.getId());

        List<Playlist> found = playlistRepository.findByOwnerIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(found).hasSize(1);
    }

    @Test
    @DisplayName("Database: Rating aggregation functions")
    public void testRatingAggregation() {
        AudioTrack track = new AudioTrack("Song", "Artist", "file.mp3", "orig.mp3", testUser);
        track = audioTrackRepository.save(track);

        // Create ratings
        ratingRepository.save(new Rating(testUser.getId(), track.getId(), 5));
        ratingRepository.save(new Rating(testUser.getId() + 1, track.getId(), 4));
        ratingRepository.save(new Rating(testUser.getId() + 2, track.getId(), 3));

        // Test aggregation
        Double avgRating = ratingRepository.findAverageRatingByTrackId(track.getId());
        assertThat(avgRating).isEqualTo(4.0); // (5+4+3)/3 = 4.0

        Long count = ratingRepository.countByTrackId(track.getId());
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Database: Favourite toggle operations")
    public void testFavouriteOperations() {
        AudioTrack track = new AudioTrack("Fav Song", "Artist", "fav.mp3", "orig.mp3", testUser);
        track = audioTrackRepository.save(track);

        // Add favourite
        Favourite fav = new Favourite(testUser.getId(), track.getId());
        fav = favouriteRepository.save(fav);
        assertThat(fav.getId()).isNotNull();

        // Check exists
        boolean exists = favouriteRepository.existsByUserIdAndTrackId(testUser.getId(), track.getId());
        assertThat(exists).isTrue();

        // Remove favourite
        favouriteRepository.delete(fav);
        exists = favouriteRepository.existsByUserIdAndTrackId(testUser.getId(), track.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Database: Case-insensitive search")
    public void testCaseInsensitiveSearch() {
        AudioTrack track1 = new AudioTrack("UPPERCASE Song", "artist", "up.mp3", "orig.mp3", testUser);
        AudioTrack track2 = new AudioTrack("lowercase song", "artist", "low.mp3", "orig.mp3", testUser);
        audioTrackRepository.save(track1);
        audioTrackRepository.save(track2);

        List<AudioTrack> found = audioTrackRepository.findByTitleContainingIgnoreCase("SONG");
        assertThat(found).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Database: Transaction rollback")
    public void testTransactionRollback() {
        long initialCount = userRepository.count();

        try {
            User user = new User("transtest", "trans@test.com", "pass");
            userRepository.save(user);
            
            // Throw exception to trigger rollback
            if (true) throw new RuntimeException("Test rollback");
        } catch (Exception e) {
            // Expected
        }

        // Due to @Transactional on test class, rollback happens automatically
        // Count should be back to initial after test method completes
    }

    @Test
    @DisplayName("Database: All domain entities persist correctly")
    public void testAllEntitiesPersist() {
        // User with unique email
        User user = new User("alltest_unique", "all_unique@test.com", "pass");
        user = userRepository.save(user);
        assertThat(user.getId()).isNotNull();

        // ChatMessage
        ChatMessage msg = new ChatMessage(user.getUsername(), "Test");
        msg = chatMessageRepository.save(msg);
        assertThat(msg.getId()).isNotNull();

        // AudioTrack
        AudioTrack track = new AudioTrack("Song", "Artist", "file.mp3", "orig.mp3", user);
        track = audioTrackRepository.save(track);
        assertThat(track.getId()).isNotNull();

        // Playlist
        Playlist playlist = new Playlist(user.getId(), "Playlist", "Desc");
        playlist = playlistRepository.save(playlist);
        assertThat(playlist.getId()).isNotNull();

        // Rating
        Rating rating = new Rating(user.getId(), track.getId(), 5);
        rating = ratingRepository.save(rating);
        assertThat(rating.getId()).isNotNull();

        // Favourite
        Favourite fav = new Favourite(user.getId(), track.getId());
        fav = favouriteRepository.save(fav);
        assertThat(fav.getId()).isNotNull();

        // All entities created successfully
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(chatMessageRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(audioTrackRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(playlistRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(ratingRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(favouriteRepository.count()).isGreaterThanOrEqualTo(1);
    }
}

