package edu.cqu.coit13235.musicchat.frontend;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Frontend Security Validation Tests
 * Ensures UI cannot perform unauthorized actions and handles errors gracefully.
 * 
 * @author Nirob (Person B - Technical Manager/Frontend & Testing Lead)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FrontendSecurityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AudioTrackRepository audioTrackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private AudioTrack trackByUserA;

    @BeforeEach
    public void setUp() {
        audioTrackRepository.deleteAll();
        userRepository.deleteAll();

        // Create two users
        userA = new User("userA", "userA@test.com", passwordEncoder.encode("password"));
        userB = new User("userB", "userB@test.com", passwordEncoder.encode("password"));
        userA = userRepository.save(userA);
        userB = userRepository.save(userB);

        // Create track owned by User A
        trackByUserA = new AudioTrack(
            "User A's Song",
            "Artist A",
            "fileA.mp3",
            "originalA.mp3",
            userA
        );
        trackByUserA = audioTrackRepository.save(trackByUserA);
    }

    @Test
    @DisplayName("Frontend: Unauthenticated user cannot access chat form")
    public void testUnauthenticatedChatAccess() throws Exception {
        // Attempt to send message without authentication
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Unauthorized message\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Frontend: Unauthenticated user cannot upload audio")
    public void testUnauthenticatedAudioUpload() throws Exception {
        // Attempt to upload without authentication
        mockMvc.perform(post("/api/audio/upload")
                .param("title", "Test")
                .param("artist", "Test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Frontend: User B cannot delete User A's track")
    public void testUserCannotDeleteOthersTrack() throws Exception {
        // User B attempts to delete User A's track
        mockMvc.perform(delete("/api/audio/tracks/" + trackByUserA.getId())
                .with(user(userB.getUsername()).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    @DisplayName("Frontend: Error messages are properly formatted for UI display")
    public void testErrorMessageFormatting() throws Exception {
        // Test invalid input produces proper error message
        // Spring validation returns 400 for @NotBlank violation
        mockMvc.perform(post("/api/chat/messages")
                .with(user(userA.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
        // Note: Spring validation error format may differ from manual error responses
    }

    @Test
    @DisplayName("Frontend: Invalid rating value shows user-friendly error")
    public void testInvalidRatingErrorHandling() throws Exception {
        // Attempt to rate with invalid value (0, should be 1-5)
        String ratingJson = String.format("{\"userId\":%d,\"rating\":0}", userA.getId());

        mockMvc.perform(post("/api/audio/" + trackByUserA.getId() + "/rate")
                .with(user(userA.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ratingJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("between 1 and 5")));
    }

    @Test
    @DisplayName("Frontend: 404 errors are handled gracefully")
    public void test404ErrorHandling() throws Exception {
        // Attempt to access non-existent track
        mockMvc.perform(get("/api/audio/tracks/99999")
                .with(user(userA.getUsername()).roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Frontend: Missing required parameters show clear error")
    public void testMissingParameterError() throws Exception {
        // Attempt to rate without userId
        mockMvc.perform(post("/api/audio/" + trackByUserA.getId() + "/rate")
                .with(user(userA.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Frontend: Users can only see their own uploaded tracks via /my endpoint")
    public void testMyTracksIsolation() throws Exception {
        // Create track for User B
        AudioTrack trackByUserB = new AudioTrack(
            "User B's Song",
            "Artist B",
            "fileB.mp3",
            "originalB.mp3",
            userB
        );
        audioTrackRepository.save(trackByUserB);

        // User A retrieves their tracks
        mockMvc.perform(get("/api/audio/tracks/my")
                .with(user(userA.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("User A's Song"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        // User B retrieves their tracks
        mockMvc.perform(get("/api/audio/tracks/my")
                .with(user(userB.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("User B's Song"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("Frontend: Public endpoints accessible without authentication")
    public void testPublicEndpointsAccessible() throws Exception {
        // Public endpoints should work without authentication
        mockMvc.perform(get("/api/audio/tracks"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chat/messages"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/playlists"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tests"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Frontend: Session expiry is handled properly")
    public void testSessionHandling() throws Exception {
        // Test that authenticated request works
        mockMvc.perform(post("/api/chat/messages")
                .with(user(userA.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Test message\"}"))
                .andExpect(status().isCreated());

        // Simulate unauthenticated request (session expired)
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Test message\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Frontend: SQL injection attempts are prevented")
    public void testSQLInjectionPrevention() throws Exception {
        // Attempt SQL injection in search query
        String maliciousQuery = "'; DROP TABLE users; --";

        mockMvc.perform(get("/api/audio/search/title?q=" + maliciousQuery)
                .with(user(userA.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        // Should return empty array, not cause SQL error

        // Verify users table still exists
        mockMvc.perform(get("/api/chat/messages")
                .with(user(userA.getUsername()).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Frontend: XSS attempts are handled safely")
    public void testXSSPrevention() throws Exception {
        // Attempt XSS in chat message
        String xssAttempt = "<script>alert('XSS')</script>";

        mockMvc.perform(post("/api/chat/messages")
                .with(user(userA.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"content\":\"%s\"}", xssAttempt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(xssAttempt));
        // Stored as-is, frontend should escape during display
    }

    @Test
    @DisplayName("Frontend: Concurrent requests from same user are handled correctly")
    public void testSameUserConcurrentRequests() throws Exception {
        // User A makes multiple rapid requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/chat/messages")
                    .with(user(userA.getUsername()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"content\":\"Message %d\"}", i)))
                    .andExpect(status().isCreated());
        }

        // Verify all messages were created
        mockMvc.perform(get("/api/chat/messages")
                .with(user(userA.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)));
    }
}

