package edu.cqu.coit13235.musicchat.concurrency;

import edu.cqu.coit13235.musicchat.service.ExternalMusicService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Concurrency Test for MusicChat Application
 * 
 * Simulates 10 concurrent users performing:
 * - Login with seeded accounts
 * - Search queries
 * - Rating submissions
 * - Favourite toggles
 * 
 * Logs all test output to /tests/concurrency/concurrency_test.log
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyTest.class);
    
    @Autowired
    private ExternalMusicService externalMusicService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080";
    
    // Seeded user accounts for testing
    private static final String[][] SEEDED_USERS = {
        {"admin", "admin123", "1"},
        {"alice", "password", "2"},
        {"bob", "password", "3"},
        {"charlie", "password", "4"},
        {"diana", "password", "5"},
        {"eve", "password", "6"},
        {"frank", "password", "7"},
        {"grace", "password", "8"},
        {"henry", "password", "9"},
        {"iris", "password", "10"}
    };
    
    // Search keywords for testing
    private static final String[] SEARCH_KEYWORDS = {
        "queen", "adele", "beatles", "michael jackson", "madonna",
        "elvis", "bob dylan", "led zeppelin", "pink floyd", "rolling stones"
    };
    
    // Log file path
    private static final String LOG_FILE_PATH = "tests/concurrency/concurrency_test.log";
    
    @Test
    public void testConcurrentUsers() throws InterruptedException, IOException {
        logger.info("🚀 Starting Concurrency Test - 10 Concurrent Users");
        
        // Create log directory if it doesn't exist
        Files.createDirectories(Paths.get("tests/concurrency"));
        
        // Clear previous log file
        try (FileWriter writer = new FileWriter(LOG_FILE_PATH, false)) {
            writer.write("=== MusicChat Concurrency Test Log ===\n");
            writer.write("Started at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");
        }
        
        // Create thread pool for 10 concurrent users
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<UserTestResult>> futures = new ArrayList<>();
        
        // Submit tasks for each user
        for (int i = 0; i < 10; i++) {
            String[] user = SEEDED_USERS[i];
            String username = user[0];
            String password = user[1];
            Long userId = Long.parseLong(user[2]);
            
            Future<UserTestResult> future = executor.submit(new UserSimulator(
                username, password, userId, i, externalMusicService, restTemplate, BASE_URL
            ));
            futures.add(future);
        }
        
        // Wait for all users to complete
        List<UserTestResult> results = new ArrayList<>();
        for (Future<UserTestResult> future : futures) {
            try {
                UserTestResult result = future.get(60, TimeUnit.SECONDS); // 60 second timeout
                results.add(result);
            } catch (TimeoutException e) {
                logger.error("❌ User test timed out");
                logToFile("❌ User test timed out: " + e.getMessage());
            } catch (ExecutionException e) {
                logger.error("❌ User test failed: " + e.getCause().getMessage());
                logToFile("❌ User test failed: " + e.getCause().getMessage());
            }
        }
        
        executor.shutdown();
        
        // Generate summary report
        generateSummaryReport(results);
        
        logger.info("✅ Concurrency test completed. Check log file: " + LOG_FILE_PATH);
    }
    
    /**
     * User simulator that performs all required operations
     */
    private static class UserSimulator implements Callable<UserTestResult> {
        private final String username;
        private final String password;
        private final Long userId;
        private final int userIndex;
        private final ExternalMusicService externalMusicService;
        private final RestTemplate restTemplate;
        private final String baseUrl;
        
        public UserSimulator(String username, String password, Long userId, int userIndex,
                           ExternalMusicService externalMusicService, RestTemplate restTemplate, String baseUrl) {
            this.username = username;
            this.password = password;
            this.userId = userId;
            this.userIndex = userIndex;
            this.externalMusicService = externalMusicService;
            this.restTemplate = restTemplate;
            this.baseUrl = baseUrl;
        }
        
        @Override
        public UserTestResult call() throws Exception {
            UserTestResult result = new UserTestResult(username, userId);
            
            try {
                logToFile("👤 User " + userIndex + " (" + username + ") started");
                
                // 1. Login simulation (verify user exists)
                result.loginSuccess = simulateLogin();
                logToFile("🔐 User " + userIndex + " login: " + (result.loginSuccess ? "SUCCESS" : "FAILED"));
                
                if (!result.loginSuccess) {
                    return result;
                }
                
                // 2. Perform search query
                String searchKeyword = SEARCH_KEYWORDS[userIndex % SEARCH_KEYWORDS.length];
                result.searchSuccess = performSearch(searchKeyword);
                logToFile("🔍 User " + userIndex + " search for '" + searchKeyword + "': " + 
                         (result.searchSuccess ? "SUCCESS" : "FAILED"));
                
                // 3. Submit rating (using demo track ID)
                Long trackId = 1L + (userIndex % 3); // Use tracks 1, 2, or 3
                Integer rating = 1 + (userIndex % 5); // Rating 1-5
                result.ratingSuccess = submitRating(trackId, rating);
                logToFile("⭐ User " + userIndex + " rating track " + trackId + " with " + rating + " stars: " + 
                         (result.ratingSuccess ? "SUCCESS" : "FAILED"));
                
                // 4. Toggle favourite
                result.favouriteSuccess = toggleFavourite(trackId);
                logToFile("❤️ User " + userIndex + " toggle favourite for track " + trackId + ": " + 
                         (result.favouriteSuccess ? "SUCCESS" : "FAILED"));
                
                // 5. Show track details with rating and favourite status
                showTrackDetails(trackId);
                
                result.completed = true;
                logToFile("✅ User " + userIndex + " (" + username + ") completed all operations");
                
            } catch (Exception e) {
                result.error = e.getMessage();
                logToFile("❌ User " + userIndex + " (" + username + ") failed: " + e.getMessage());
            }
            
            return result;
        }
        
        private boolean simulateLogin() {
            try {
                // Simulate login by making an authenticated request
                String url = baseUrl + "/api/audio/tracks";
                HttpHeaders headers = createAuthHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );
                
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                return false;
            }
        }
        
        private boolean performSearch(String keyword) {
            try {
                List<ExternalMusicService.MusicMetadata> results = externalMusicService.searchMusic(keyword);
                return results != null && !results.isEmpty();
            } catch (Exception e) {
                return false;
            }
        }
        
        private boolean submitRating(Long trackId, Integer rating) {
            try {
                String url = baseUrl + "/api/audio/" + trackId + "/rate";
                Map<String, Object> requestBody = Map.of(
                    "userId", userId,
                    "rating", rating
                );
                
                HttpHeaders headers = createAuthHeaders();
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
                );
                
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                return false;
            }
        }
        
        private boolean toggleFavourite(Long trackId) {
            try {
                String url = baseUrl + "/api/audio/" + trackId + "/favorite";
                Map<String, Object> requestBody = Map.of("userId", userId);
                
                HttpHeaders headers = createAuthHeaders();
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
                );
                
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                return false;
            }
        }
        
        private void showTrackDetails(Long trackId) {
            try {
                // Get rating details
                String ratingUrl = baseUrl + "/api/audio/" + trackId + "/ratings";
                HttpHeaders headers = createAuthHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<Map> ratingResponse = restTemplate.exchange(
                    ratingUrl, HttpMethod.GET, entity, Map.class
                );
                
                if (ratingResponse.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> ratingData = ratingResponse.getBody();
                    logToFile("📊 User " + userIndex + " - Track " + trackId + " rating details: " + 
                             "Average: " + ratingData.get("averageRating") + 
                             ", Count: " + ratingData.get("ratingCount"));
                }
                
                // Get favourite details
                String favUrl = baseUrl + "/api/audio/" + trackId + "/favourites";
                ResponseEntity<Map> favResponse = restTemplate.exchange(
                    favUrl, HttpMethod.GET, entity, Map.class
                );
                
                if (favResponse.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> favData = favResponse.getBody();
                    logToFile("❤️ User " + userIndex + " - Track " + trackId + " favourite count: " + 
                             favData.get("favouriteCount"));
                }
                
            } catch (Exception e) {
                logToFile("⚠️ User " + userIndex + " - Failed to get track details: " + e.getMessage());
            }
        }
        
        private HttpHeaders createAuthHeaders() {
            HttpHeaders headers = new HttpHeaders();
            String credentials = username + ":" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
            headers.set("Content-Type", "application/json");
            return headers;
        }
    }
    
    /**
     * Result class for user test operations
     */
    private static class UserTestResult {
        final String username;
        final Long userId;
        boolean loginSuccess = false;
        boolean searchSuccess = false;
        boolean ratingSuccess = false;
        boolean favouriteSuccess = false;
        boolean completed = false;
        String error = null;
        
        public UserTestResult(String username, Long userId) {
            this.username = username;
            this.userId = userId;
        }
    }
    
    /**
     * Generate summary report
     */
    private void generateSummaryReport(List<UserTestResult> results) throws IOException {
        try (FileWriter writer = new FileWriter(LOG_FILE_PATH, true)) {
            writer.write("\n=== CONCURRENCY TEST SUMMARY ===\n");
            writer.write("Completed at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("Total users tested: " + results.size() + "\n\n");
            
            int successfulUsers = 0;
            int totalOperations = 0;
            int successfulOperations = 0;
            
            for (UserTestResult result : results) {
                writer.write("User: " + result.username + " (ID: " + result.userId + ")\n");
                writer.write("  Login: " + (result.loginSuccess ? "✅" : "❌") + "\n");
                writer.write("  Search: " + (result.searchSuccess ? "✅" : "❌") + "\n");
                writer.write("  Rating: " + (result.ratingSuccess ? "✅" : "❌") + "\n");
                writer.write("  Favourite: " + (result.favouriteSuccess ? "✅" : "❌") + "\n");
                writer.write("  Completed: " + (result.completed ? "✅" : "❌") + "\n");
                if (result.error != null) {
                    writer.write("  Error: " + result.error + "\n");
                }
                writer.write("\n");
                
                if (result.completed) successfulUsers++;
                totalOperations += 4; // login, search, rating, favourite
                if (result.loginSuccess) successfulOperations++;
                if (result.searchSuccess) successfulOperations++;
                if (result.ratingSuccess) successfulOperations++;
                if (result.favouriteSuccess) successfulOperations++;
            }
            
            writer.write("=== STATISTICS ===\n");
            writer.write("Successful users: " + successfulUsers + "/" + results.size() + "\n");
            writer.write("Success rate: " + String.format("%.1f%%", (double) successfulUsers / results.size() * 100) + "\n");
            writer.write("Successful operations: " + successfulOperations + "/" + totalOperations + "\n");
            writer.write("Operation success rate: " + String.format("%.1f%%", (double) successfulOperations / totalOperations * 100) + "\n");
            
            // Test conclusion
            if (successfulUsers == results.size()) {
                writer.write("\n🎉 CONCURRENCY TEST PASSED - All users completed successfully!\n");
            } else if (successfulUsers >= results.size() * 0.8) {
                writer.write("\n⚠️ CONCURRENCY TEST PARTIALLY PASSED - Most users completed successfully\n");
            } else {
                writer.write("\n❌ CONCURRENCY TEST FAILED - Many users failed to complete operations\n");
            }
        }
    }
    
    /**
     * Log message to file
     */
    private static void logToFile(String message) {
        try (FileWriter writer = new FileWriter(LOG_FILE_PATH, true)) {
            writer.write("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME) + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
