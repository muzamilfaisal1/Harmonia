package edu.cqu.coit13235.musicchat.runner;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import edu.cqu.coit13235.musicchat.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConsoleRunner functionality.
 * Demonstrates how the ConsoleRunner can be used in automated tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConsoleRunnerTest {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private AudioService audioService;
    
    @Autowired
    private RatingService ratingService;
    
    @Autowired
    private FavouriteService favouriteService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private MusicChatConsoleRunner consoleRunner;
    
    private List<User> testUsers;
    private AudioTrack testTrack;
    
    @BeforeEach
    public void setUp() {
        // Create test users for console runner tests
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User user = new User(
                "consoleUser" + i,
                "console" + i + "@test.com",
                passwordEncoder.encode("password")
            );
            testUsers.add(userRepository.save(user));
        }
        
        // Create a test track
        testTrack = new AudioTrack(
            "Console Test Song",
            "Test Artist",
            "console_test.mp3",
            "original.mp3",
            testUsers.get(0)
        );
        testTrack = audioTrackRepository.save(testTrack);
    }
    
    @Test
    public void testConsoleRunnerServicesAreAvailable() {
        // Test that all required services are properly injected
        assertNotNull(chatService, "ChatService should be available");
        assertNotNull(audioService, "AudioService should be available");
        assertNotNull(ratingService, "RatingService should be available");
        assertNotNull(favouriteService, "FavouriteService should be available");
        assertNotNull(userRepository, "UserRepository should be available");
        assertNotNull(consoleRunner, "ConsoleRunner should be available");
    }
    
    @Test
    public void testUserActionsCanBeSimulated() {
        // Get a test user
        List<User> users = userRepository.findAll();
        assertFalse(users.isEmpty(), "Should have at least one user for testing");
        
        User testUser = users.get(0);
        
        // Test chat message sending
        long initialMessageCount = chatService.getMessageCount();
        chatService.sendMessage(testUser.getUsername(), "Test message from automated test");
        assertEquals(initialMessageCount + 1, chatService.getMessageCount(), 
                    "Message count should increase by 1");
        
        // Test rating functionality (if tracks exist)
        List<AudioTrack> tracks = audioService.getAllTracks();
        if (!tracks.isEmpty()) {
            AudioTrack track = tracks.get(0);
            ratingService.rateTrack(testUser.getId(), track.getId(), 5);
            
            // Verify rating was created
            assertTrue(ratingService.getUserRating(testUser.getId(), track.getId()).isPresent(),
                      "User rating should exist after rating a track");
        }
        
        // Test favourite functionality (if tracks exist)
        if (!tracks.isEmpty()) {
            AudioTrack track = tracks.get(0);
            boolean wasFavourited = favouriteService.toggleFavourite(testUser.getId(), track.getId());
            
            // Verify favourite was created
            assertTrue(favouriteService.isFavourited(testUser.getId(), track.getId()),
                      "Track should be favourited after toggle");
        }
    }
    
    @Test
    public void testMultipleUsersCanPerformActions() {
        // Get multiple users
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 2, "Should have at least 2 users for multi-user testing");
        
        // Test that multiple users can send messages
        long initialMessageCount = chatService.getMessageCount();
        
        for (User user : users.subList(0, Math.min(3, users.size()))) {
            chatService.sendMessage(user.getUsername(), "Test message from " + user.getUsername());
        }
        
        assertEquals(initialMessageCount + Math.min(3, users.size()), chatService.getMessageCount(),
                    "Message count should increase by the number of users who sent messages");
    }
    
    @Test
    public void testConcurrentUserActions() throws InterruptedException {
        // This test simulates concurrent actions similar to what the ConsoleRunner does
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 2, "Should have at least 2 users for concurrent testing");
        
        long initialMessageCount = chatService.getMessageCount();
        
        // Create threads to simulate concurrent actions
        Thread[] threads = new Thread[Math.min(3, users.size())];
        
        for (int i = 0; i < threads.length; i++) {
            final User user = users.get(i);
            threads[i] = new Thread(() -> {
                try {
                    // Simulate multiple actions per user
                    for (int j = 0; j < 3; j++) {
                        chatService.sendMessage(user.getUsername(), 
                            "Concurrent test message " + j + " from " + user.getUsername());
                        Thread.sleep(10); // Small delay to simulate real usage
                    }
                } catch (Exception e) {
                    fail("Concurrent action failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all messages were sent
        long expectedMessages = initialMessageCount + (threads.length * 3);
        assertEquals(expectedMessages, chatService.getMessageCount(),
                    "All concurrent messages should have been sent");
    }
}
