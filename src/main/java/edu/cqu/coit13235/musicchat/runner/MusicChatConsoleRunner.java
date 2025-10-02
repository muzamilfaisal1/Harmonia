package edu.cqu.coit13235.musicchat.runner;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import edu.cqu.coit13235.musicchat.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Console Runner for MusicChat that simulates multiple users performing actions.
 * Implements CommandLineRunner to execute automatically when the app starts.
 * 
 * Features:
 * - Simulates 10 users performing concurrent actions
 * - Sends chat messages through ChatService
 * - Uploads audio files through AudioService
 * - Rates and favourites tracks through RatingService and FavouriteService
 * - Uses proper authentication context for each user
 * - Provides console output for tracking actions
 * - Reusable for automated testing
 */
@Component
public class MusicChatConsoleRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MusicChatConsoleRunner.class);
    
    // Services
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
    private UserDetailsService userDetailsService;
    
    // Configuration
    private static final int SIMULATED_USERS = 10;
    private static final int ACTIONS_PER_USER = 5;
    private static final int CONCURRENT_THREADS = 5;
    
    // Sample data for simulation
    private static final String[] SAMPLE_MESSAGES = {
        "Hey everyone! What's your favorite genre?",
        "I just discovered this amazing new artist!",
        "Has anyone heard the latest album from The Weeknd?",
        "I'm looking for some good workout music recommendations",
        "This song is stuck in my head all day!",
        "Anyone up for a music trivia challenge?",
        "I love how music brings people together",
        "What's the best concert you've ever been to?",
        "I'm trying to learn guitar, any tips?",
        "Music is the universal language of emotions"
    };
    
    private static final String[] SAMPLE_TRACKS = {
        "Bohemian Rhapsody", "Imagine", "Hotel California", "Stairway to Heaven", "Billie Jean",
        "Sweet Child O' Mine", "Smells Like Teen Spirit", "Like a Rolling Stone", "Yesterday", "Hey Jude"
    };
    
    private static final String[] SAMPLE_ARTISTS = {
        "Queen", "John Lennon", "Eagles", "Led Zeppelin", "Michael Jackson",
        "Guns N' Roses", "Nirvana", "Bob Dylan", "The Beatles", "The Beatles"
    };
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("🎵 MusicChat Multi-User Simulation Console Runner Started");
        logger.info("Simulating {} users performing {} actions each", SIMULATED_USERS, ACTIONS_PER_USER);
        logger.info("Using {} concurrent threads for simulation", CONCURRENT_THREADS);
        logger.info("");
        
        // Wait a bit for the application to fully start
        Thread.sleep(2000);
        
        try {
            // Run the multi-user simulation
            runMultiUserSimulation();
            
            logger.info("✅ Multi-user simulation completed successfully!");
            logger.info("Check the console output above to see all simulated actions.");
            
        } catch (Exception e) {
            logger.error("❌ Error during multi-user simulation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Run multi-user simulation with concurrent actions
     */
    private void runMultiUserSimulation() throws Exception {
        // Get all available users
        List<User> users = userRepository.findAll();
        if (users.size() < SIMULATED_USERS) {
            logger.warn("Only {} users available, but {} requested. Using available users.", users.size(), SIMULATED_USERS);
        }
        
        // Limit to available users
        List<User> selectedUsers = users.subList(0, Math.min(SIMULATED_USERS, users.size()));
        logger.info("Selected {} users for simulation: {}", selectedUsers.size(), 
                   selectedUsers.stream().map(User::getUsername).toArray());
        
        // Create thread pool for concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Submit tasks for each user
        for (User user : selectedUsers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    simulateUserActions(user);
                } catch (Exception e) {
                    logger.error("Error simulating actions for user {}: {}", user.getUsername(), e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Shutdown executor
        executor.shutdown();
        
        // Print final statistics
        printSimulationStatistics();
    }
    
    /**
     * Simulate actions for a single user
     */
    private void simulateUserActions(User user) throws Exception {
        logger.info("👤 Starting simulation for user: {}", user.getUsername());
        
        // Set authentication context for this user
        setAuthenticationContext(user);
        
        // Perform random actions
        for (int i = 0; i < ACTIONS_PER_USER; i++) {
            try {
                // Random delay between actions (100-500ms)
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                
                // Randomly choose an action
                ActionType action = ActionType.values()[ThreadLocalRandom.current().nextInt(ActionType.values().length)];
                
                switch (action) {
                    case SEND_MESSAGE:
                        sendChatMessage(user);
                        break;
                    case UPLOAD_AUDIO:
                        uploadAudioFile(user);
                        break;
                    case RATE_TRACK:
                        rateAudioTrack(user);
                        break;
                    case FAVOURITE_TRACK:
                        favouriteAudioTrack(user);
                        break;
                }
                
            } catch (Exception e) {
                logger.warn("Action failed for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
        
        logger.info("✅ Completed simulation for user: {}", user.getUsername());
    }
    
    /**
     * Set authentication context for a user
     */
    private void setAuthenticationContext(User user) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            logger.error("Failed to set authentication context for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Send a chat message
     */
    private void sendChatMessage(User user) {
        try {
            String message = SAMPLE_MESSAGES[ThreadLocalRandom.current().nextInt(SAMPLE_MESSAGES.length)];
            chatService.sendMessage(user.getUsername(), message);
            logger.info("💬 {} sent message: \"{}\"", user.getUsername(), message);
        } catch (Exception e) {
            logger.warn("Failed to send message for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Upload an audio file
     */
    private void uploadAudioFile(User user) {
        try {
            String title = SAMPLE_TRACKS[ThreadLocalRandom.current().nextInt(SAMPLE_TRACKS.length)];
            String artist = SAMPLE_ARTISTS[ThreadLocalRandom.current().nextInt(SAMPLE_ARTISTS.length)];
            
            // Create a mock audio file
            byte[] audioData = generateMockAudioData();
            MultipartFile file = new SimpleMultipartFile(
                "file", 
                title.replaceAll("\\s+", "_") + ".mp3", 
                "audio/mpeg", 
                audioData
            );
            
            AudioTrack track = audioService.uploadAudio(file, title, artist);
            logger.info("🎵 {} uploaded track: \"{}\" by {}", user.getUsername(), title, artist);
        } catch (Exception e) {
            logger.warn("Failed to upload audio for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Rate an audio track
     */
    private void rateAudioTrack(User user) {
        try {
            // Get a random existing track
            List<AudioTrack> tracks = audioService.getAllTracks();
            if (tracks.isEmpty()) {
                logger.debug("No tracks available for rating by user {}", user.getUsername());
                return;
            }
            
            AudioTrack track = tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
            int rating = ThreadLocalRandom.current().nextInt(1, 6); // 1-5 stars
            
            ratingService.rateTrack(user.getId(), track.getId(), rating);
            logger.info("⭐ {} rated track \"{}\" with {} stars", user.getUsername(), track.getTitle(), rating);
        } catch (Exception e) {
            logger.warn("Failed to rate track for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Favourite an audio track
     */
    private void favouriteAudioTrack(User user) {
        try {
            // Get a random existing track
            List<AudioTrack> tracks = audioService.getAllTracks();
            if (tracks.isEmpty()) {
                logger.debug("No tracks available for favouriting by user {}", user.getUsername());
                return;
            }
            
            AudioTrack track = tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
            boolean isFavourited = favouriteService.toggleFavourite(user.getId(), track.getId());
            
            if (isFavourited) {
                logger.info("❤️ {} added track \"{}\" to favourites", user.getUsername(), track.getTitle());
            } else {
                logger.info("💔 {} removed track \"{}\" from favourites", user.getUsername(), track.getTitle());
            }
        } catch (Exception e) {
            logger.warn("Failed to toggle favourite for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * Generate mock audio data for testing
     */
    private byte[] generateMockAudioData() {
        // Generate a small mock audio file (1KB)
        byte[] data = new byte[1024];
        ThreadLocalRandom.current().nextBytes(data);
        return data;
    }
    
    /**
     * Print simulation statistics
     */
    private void printSimulationStatistics() {
        try {
            long totalTracks = audioService.getTrackCount();
            long totalMessages = chatService.getMessageCount();
            
            logger.info("");
            logger.info("📊 Simulation Statistics:");
            logger.info("   Total tracks in system: {}", totalTracks);
            logger.info("   Total chat messages: {}", totalMessages);
            logger.info("   Users simulated: {}", SIMULATED_USERS);
            logger.info("   Actions per user: {}", ACTIONS_PER_USER);
            logger.info("   Total actions performed: {}", SIMULATED_USERS * ACTIONS_PER_USER);
        } catch (Exception e) {
            logger.error("Failed to print statistics: {}", e.getMessage());
        }
    }
    
    /**
     * Enum for action types
     */
    private enum ActionType {
        SEND_MESSAGE,
        UPLOAD_AUDIO,
        RATE_TRACK,
        FAVOURITE_TRACK
    }
    
    /**
     * Simple implementation of MultipartFile for testing purposes
     */
    private static class SimpleMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;
        
        public SimpleMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }
        
        @Override
        public byte[] getBytes() {
            return content;
        }
        
        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
