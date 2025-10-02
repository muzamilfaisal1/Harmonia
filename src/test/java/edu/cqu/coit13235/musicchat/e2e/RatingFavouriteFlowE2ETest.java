package edu.cqu.coit13235.musicchat.e2e;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Favourite;
import edu.cqu.coit13235.musicchat.domain.Rating;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.FavouriteRepository;
import edu.cqu.coit13235.musicchat.repository.RatingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End tests for Rating & Favourite Flow.
 * Tests: Toggle rating → toggle favourite → verify persistence → retrieve status.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RatingFavouriteFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @Autowired
    private AudioTrackRepository audioTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private AudioTrack testTrack;

    @BeforeEach
    public void setUp() {
        ratingRepository.deleteAll();
        favouriteRepository.deleteAll();
        audioTrackRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);

        testTrack = new AudioTrack("Test Song", "Test Artist", "test.mp3", "original.mp3", testUser);
        testTrack = audioTrackRepository.save(testTrack);
    }

    @Test
    @DisplayName("E2E: Toggle rating → verify persistence → retrieve rating")
    public void testRatingFlowEndToEnd() throws Exception {
        // Step 1: Rate a track with 5 stars
        String rateJson = String.format("{\"userId\":%d,\"rating\":5}", testUser.getId());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/rate")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(rateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating.ratingValue").value(5))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1));

        // Step 2: Verify persistence in H2
        List<Rating> ratings = ratingRepository.findAll();
        assertThat(ratings).hasSize(1);
        assertThat(ratings.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(ratings.get(0).getTrackId()).isEqualTo(testTrack.getId());
        assertThat(ratings.get(0).getRatingValue()).isEqualTo(5);

        // Step 3: Retrieve rating statistics
        mockMvc.perform(get("/api/audio/" + testTrack.getId() + "/ratings")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1));

        // Step 4: Update rating to 3 stars
        String updateJson = String.format("{\"userId\":%d,\"rating\":3}", testUser.getId());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/rate")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating.ratingValue").value(3))
                .andExpect(jsonPath("$.averageRating").value(3.0));

        // Verify updated rating in H2
        ratings = ratingRepository.findAll();
        assertThat(ratings).hasSize(1); // Should still be 1, just updated
        assertThat(ratings.get(0).getRatingValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("E2E: Toggle favourite on/off → verify persistence")
    public void testFavouriteToggleFlowEndToEnd() throws Exception {
        // Step 1: Add track to favourites
        String favouriteJson = String.format("{\"userId\":%d}", testUser.getId());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavourited").value(true))
                .andExpect(jsonPath("$.favouriteCount").value(1))
                .andExpect(jsonPath("$.message").value("Track added to favourites"));

        // Step 2: Verify persistence in H2
        List<Favourite> favourites = favouriteRepository.findAll();
        assertThat(favourites).hasSize(1);
        assertThat(favourites.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(favourites.get(0).getTrackId()).isEqualTo(testTrack.getId());

        // Step 3: Retrieve favourite count
        mockMvc.perform(get("/api/audio/" + testTrack.getId() + "/favourites")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favouriteCount").value(1));

        // Step 4: Toggle favourite off (remove from favourites)
        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavourited").value(false))
                .andExpect(jsonPath("$.favouriteCount").value(0))
                .andExpect(jsonPath("$.message").value("Track removed from favourites"));

        // Verify removal from H2
        favourites = favouriteRepository.findAll();
        assertThat(favourites).isEmpty();

        // Step 5: Toggle favourite back on
        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavourited").value(true));

        // Verify re-addition
        favourites = favouriteRepository.findAll();
        assertThat(favourites).hasSize(1);
    }

    @Test
    @DisplayName("E2E: Multiple users rating and favouriting the same track")
    public void testMultipleUsersInteractions() throws Exception {
        // Create second user
        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password"));
        user2 = userRepository.save(user2);

        // User 1 rates 5 stars and favourites
        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/rate")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":%d,\"rating\":5}", testUser.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":%d}", testUser.getId())))
                .andExpect(status().isOk());

        // User 2 rates 3 stars and favourites
        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/rate")
                .with(user(user2.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":%d,\"rating\":3}", user2.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(user2.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":%d}", user2.getId())))
                .andExpect(status().isOk());

        // Verify ratings in H2
        List<Rating> ratings = ratingRepository.findAll();
        assertThat(ratings).hasSize(2);

        // Verify favourites in H2
        List<Favourite> favourites = favouriteRepository.findAll();
        assertThat(favourites).hasSize(2);

        // Verify average rating is 4.0 ((5+3)/2)
        mockMvc.perform(get("/api/audio/" + testTrack.getId() + "/ratings")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.ratingCount").value(2));

        // Verify favourite count is 2
        mockMvc.perform(get("/api/audio/" + testTrack.getId() + "/favourites")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favouriteCount").value(2));
    }

    @Test
    @DisplayName("E2E: Retrieve user's favourite tracks")
    public void testRetrieveUserFavouriteTracks() throws Exception {
        // Create multiple tracks
        AudioTrack track2 = new AudioTrack("Song 2", "Artist 2", "file2.mp3", "orig2.mp3", testUser);
        AudioTrack track3 = new AudioTrack("Song 3", "Artist 3", "file3.mp3", "orig3.mp3", testUser);
        track2 = audioTrackRepository.save(track2);
        track3 = audioTrackRepository.save(track3);

        // Favourite tracks 1 and 3
        String favouriteJson = String.format("{\"userId\":%d}", testUser.getId());

        mockMvc.perform(post("/api/audio/" + testTrack.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audio/" + track3.getId() + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk());

        // Retrieve user's favourite tracks
        mockMvc.perform(get("/api/audio/favorites?userId=" + testUser.getId())
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2));

        // Verify favourites in H2
        List<Favourite> favourites = favouriteRepository.findByUserId(testUser.getId());
        assertThat(favourites).hasSize(2);
    }
}

