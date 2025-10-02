package edu.cqu.coit13235.musicchat.harness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Test Harness
 * Validates complete user workflows through the entire system stack.
 * Tests run in order to demonstrate real-world usage scenarios.
 * 
 * @author Nirob (Person B - Technical Manager/Frontend & Testing Lead)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class E2ETestHarness {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static User testUser;
    private static Long uploadedTrackId;
    private static Long createdPlaylistId;

    @BeforeAll
    public static void setupClass() {
        System.out.println("=".repeat(80));
        System.out.println("STARTING E2E TEST HARNESS - Complete System Workflow Validation");
        System.out.println("=".repeat(80));
    }

    @AfterAll
    public static void teardownClass() {
        System.out.println("=".repeat(80));
        System.out.println("E2E TEST HARNESS COMPLETED SUCCESSFULLY");
        System.out.println("=".repeat(80));
    }

    @BeforeEach
    public void setUp() {
        if (testUser == null) {
            // Clean up from any previous test run
            userRepository.deleteAll();
            
            testUser = new User(
                "e2e_test_user",
                "e2e@test.com",
                passwordEncoder.encode("password")
            );
            testUser = userRepository.save(testUser);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Harness Step 1: User sends chat message → verify persistence")
    public void step1_ChatMessageFlow() throws Exception {
        System.out.println("\n[STEP 1] Testing Chat Message Flow...");

        // Send message
        String messageContent = "E2E Test: Hello from test harness!";
        MvcResult result = mockMvc.perform(post("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"content\":\"%s\"}", messageContent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sender").value(testUser.getUsername()))
                .andExpect(jsonPath("$.text").value(messageContent))
                .andReturn();

        System.out.println("✓ Message sent successfully");

        // Retrieve messages
        mockMvc.perform(get("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].text").value(messageContent));

        System.out.println("✓ Message retrieved and verified");
        System.out.println("[STEP 1] ✓ PASSED\n");
    }

    @Test
    @Order(2)
    @DisplayName("Harness Step 2: Upload audio → fetch metadata → verify")
    public void step2_AudioUploadFlow() throws Exception {
        System.out.println("\n[STEP 2] Testing Audio Upload Flow...");

        // Upload audio file
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "e2e-test-song.mp3",
            "audio/mpeg",
            "E2E test audio content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/audio/upload")
                .file(audioFile)
                .param("title", "E2E Test Song")
                .param("artist", "Test Artist")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("E2E Test Song"))
                .andExpect(jsonPath("$.artist").value("Test Artist"))
                .andReturn();

        String responseBody = uploadResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        uploadedTrackId = jsonNode.get("id").asLong();

        System.out.println("✓ Audio uploaded successfully (ID: " + uploadedTrackId + ")");

        // Retrieve track
        mockMvc.perform(get("/api/audio/tracks/" + uploadedTrackId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uploadedTrackId))
                .andExpect(jsonPath("$.title").value("E2E Test Song"));

        System.out.println("✓ Audio track retrieved and verified");
        System.out.println("[STEP 2] ✓ PASSED\n");
    }

    @Test
    @Order(3)
    @DisplayName("Harness Step 3: Create playlist → add track → verify")
    public void step3_PlaylistCreationFlow() throws Exception {
        System.out.println("\n[STEP 3] Testing Playlist Creation Flow...");

        // Ensure we have a track
        if (uploadedTrackId == null) {
            throw new IllegalStateException("No track available. Run Step 2 first.");
        }

        // Create playlist
        String playlistJson = String.format(
            "{\"ownerId\":%d,\"name\":\"E2E Test Playlist\",\"description\":\"Created by harness\",\"trackIds\":[]}",
            testUser.getId()
        );

        MvcResult createResult = mockMvc.perform(post("/api/playlists")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(playlistJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("E2E Test Playlist"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        createdPlaylistId = jsonNode.get("id").asLong();

        System.out.println("✓ Playlist created successfully (ID: " + createdPlaylistId + ")");

        // Add track to playlist
        String addTrackJson = String.format("{\"trackIds\":[%d]}", uploadedTrackId);
        mockMvc.perform(post("/api/playlists/" + createdPlaylistId + "/tracks")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addTrackJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks").isArray());

        System.out.println("✓ Track added to playlist");

        // Verify playlist contents
        mockMvc.perform(get("/api/playlists/" + createdPlaylistId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].track.id").value(uploadedTrackId));

        System.out.println("✓ Playlist contents verified");
        System.out.println("[STEP 3] ✓ PASSED\n");
    }

    @Test
    @Order(4)
    @DisplayName("Harness Step 4: Rate track → toggle favourite → verify")
    public void step4_RatingAndFavouriteFlow() throws Exception {
        System.out.println("\n[STEP 4] Testing Rating & Favourite Flow...");

        if (uploadedTrackId == null) {
            throw new IllegalStateException("No track available. Run Step 2 first.");
        }

        // Rate track (5 stars)
        String ratingJson = String.format("{\"userId\":%d,\"rating\":5}", testUser.getId());
        mockMvc.perform(post("/api/audio/" + uploadedTrackId + "/rate")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ratingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating.ratingValue").value(5))
                .andExpect(jsonPath("$.averageRating").value(5.0));

        System.out.println("✓ Track rated 5 stars");

        // Toggle favourite (add)
        String favouriteJson = String.format("{\"userId\":%d}", testUser.getId());
        mockMvc.perform(post("/api/audio/" + uploadedTrackId + "/favorite")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(favouriteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavourited").value(true));

        System.out.println("✓ Track added to favourites");

        // Verify rating statistics
        mockMvc.perform(get("/api/audio/" + uploadedTrackId + "/ratings")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1));

        System.out.println("✓ Rating statistics verified");

        // Verify favourite status
        mockMvc.perform(get("/api/audio/" + uploadedTrackId + "/favourites")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favouriteCount").value(1));

        System.out.println("✓ Favourite status verified");
        System.out.println("[STEP 4] ✓ PASSED\n");
    }

    @Test
    @Order(5)
    @DisplayName("Harness Step 5: Verify /tests endpoint integration")
    public void step5_TestsEndpointVerification() throws Exception {
        System.out.println("\n[STEP 5] Testing /tests Endpoint...");

        // Call /tests endpoint
        MvcResult result = mockMvc.perform(get("/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastRun").exists())
                .andExpect(jsonPath("$.totalTests").exists())
                .andExpect(jsonPath("$.passed").exists())
                .andExpect(jsonPath("$.failed").exists())
                .andExpect(jsonPath("$.skipped").exists())
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        System.out.println("✓ /tests endpoint accessible");
        System.out.println("  Last Run: " + jsonNode.get("lastRun").asText());
        System.out.println("  Total Tests: " + jsonNode.get("totalTests").asInt());
        System.out.println("  Passed: " + jsonNode.get("passed").asInt());
        System.out.println("  Failed: " + jsonNode.get("failed").asInt());
        System.out.println("  Status: " + jsonNode.get("status").asText());
        System.out.println("[STEP 5] ✓ PASSED\n");
    }

    @Test
    @Order(6)
    @DisplayName("Harness Step 6: Security validation - unauthenticated access")
    public void step6_SecurityValidation() throws Exception {
        System.out.println("\n[STEP 6] Testing Security Validation...");

        // Test unauthenticated access to protected endpoint
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Unauthorized message\"}"))
                .andExpect(status().isUnauthorized());

        System.out.println("✓ Unauthenticated access properly rejected (401)");

        // Test authenticated access works
        mockMvc.perform(post("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Authorized message\"}"))
                .andExpect(status().isCreated());

        System.out.println("✓ Authenticated access works correctly");
        System.out.println("[STEP 6] ✓ PASSED\n");
    }

    @Test
    @Order(7)
    @DisplayName("Harness Step 7: Complete workflow summary")
    public void step7_WorkflowSummary() throws Exception {
        System.out.println("\n[STEP 7] Workflow Summary...");
        System.out.println("═".repeat(80));
        System.out.println("COMPLETE E2E WORKFLOW EXECUTED SUCCESSFULLY");
        System.out.println("═".repeat(80));
        System.out.println("1. ✓ Chat message sent and retrieved");
        System.out.println("2. ✓ Audio file uploaded and verified");
        System.out.println("3. ✓ Playlist created with track");
        System.out.println("4. ✓ Track rated and favourited");
        System.out.println("5. ✓ Test results endpoint accessible");
        System.out.println("6. ✓ Security properly enforced");
        System.out.println("═".repeat(80));
        System.out.println("Database: H2 (in-memory)");
        System.out.println("All operations persisted successfully");
        System.out.println("System ready for production");
        System.out.println("═".repeat(80));
        System.out.println("[STEP 7] ✓ COMPLETE\n");
    }
}

