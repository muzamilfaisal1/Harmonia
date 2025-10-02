# MusicChat ConsoleRunner Usage Guide

## Overview

The `MusicChatConsoleRunner` is a Spring Boot `CommandLineRunner` implementation that automatically simulates multiple users performing various actions in the MusicChat application. It's designed for testing, demonstration, and load testing purposes.

## Features

- **Multi-User Simulation**: Simulates up to 10 users performing concurrent actions
- **Automatic Execution**: Runs automatically when the Spring Boot application starts
- **Concurrent Actions**: Uses thread pools to simulate realistic concurrent user behavior
- **Comprehensive Actions**: Supports chat messages, audio uploads, ratings, and favourites
- **Proper Authentication**: Each simulated user has proper Spring Security authentication context
- **Console Output**: Provides detailed logging of all simulated actions
- **Test-Reusable**: Can be used in automated JUnit tests

## Configuration

The ConsoleRunner can be configured by modifying these constants in the class:

```java
private static final int SIMULATED_USERS = 10;        // Number of users to simulate
private static final int ACTIONS_PER_USER = 5;        // Actions each user performs
private static final int CONCURRENT_THREADS = 5;      // Thread pool size for concurrency
```

## Actions Performed

Each simulated user randomly performs these actions:

1. **Send Chat Messages**: Users send random messages to the chat
2. **Upload Audio Files**: Users upload mock audio tracks with random titles and artists
3. **Rate Tracks**: Users rate existing audio tracks with 1-5 stars
4. **Toggle Favourites**: Users add/remove tracks from their favourites

## Sample Data

The runner uses predefined sample data:

- **Messages**: 10 different music-related chat messages
- **Tracks**: 10 classic song titles (Bohemian Rhapsody, Imagine, etc.)
- **Artists**: 10 corresponding artist names (Queen, John Lennon, etc.)

## Console Output

The runner provides detailed console output showing:

```
🎵 MusicChat Multi-User Simulation Console Runner Started
Simulating 10 users performing 5 actions each
Using 5 concurrent threads for simulation

👤 Starting simulation for user: alice
💬 alice sent message: "Hey everyone! What's your favorite genre?"
🎵 alice uploaded track: "Bohemian Rhapsody" by Queen
⭐ alice rated track "Imagine" with 4 stars
❤️ alice added track "Hotel California" to favourites
✅ Completed simulation for user: alice

📊 Simulation Statistics:
   Total tracks in system: 15
   Total chat messages: 25
   Users simulated: 10
   Actions per user: 5
   Total actions performed: 50
```

## Usage in Tests

The ConsoleRunner is designed to be reusable in automated tests. Here's an example:

```java
@SpringBootTest
@ActiveProfiles("test")
public class ConsoleRunnerTest {
    
    @Autowired
    private MusicChatConsoleRunner consoleRunner;
    
    @Autowired
    private ChatService chatService;
    
    @Test
    public void testUserActionsCanBeSimulated() {
        // The ConsoleRunner services can be used directly in tests
        User testUser = userRepository.findAll().get(0);
        chatService.sendMessage(testUser.getUsername(), "Test message");
        
        // Verify the action was performed
        assertEquals(1, chatService.getMessageCount());
    }
}
```

## Authentication Context

Each simulated user has proper Spring Security authentication context:

```java
private void setAuthenticationContext(User user) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    Authentication auth = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

This ensures that all service calls are properly authenticated and authorized.

## Thread Safety

The runner uses:
- **ExecutorService**: For managing concurrent user simulations
- **CompletableFuture**: For asynchronous task execution
- **ThreadLocalRandom**: For thread-safe random number generation
- **Proper Synchronization**: All database operations are handled by Spring's transaction management

## Error Handling

The runner includes comprehensive error handling:
- Individual action failures don't stop the entire simulation
- Detailed error logging for debugging
- Graceful degradation when services are unavailable

## Performance Considerations

- **Configurable Thread Pool**: Adjust `CONCURRENT_THREADS` based on your system
- **Random Delays**: 100-500ms delays between actions simulate realistic user behavior
- **Mock Data**: Uses lightweight mock audio files (1KB) to avoid storage issues
- **Transaction Management**: All database operations are properly transactional

## Disabling the Runner

To disable the ConsoleRunner in production or specific environments:

1. **Remove @Component annotation**:
   ```java
   // @Component  // Disabled for production
   public class MusicChatConsoleRunner implements CommandLineRunner {
   ```

2. **Use Profile-based activation**:
   ```java
   @Component
   @Profile("!production")
   public class MusicChatConsoleRunner implements CommandLineRunner {
   ```

3. **Use Conditional annotation**:
   ```java
   @Component
   @ConditionalOnProperty(name = "app.console-runner.enabled", havingValue = "true", matchIfMissing = true)
   public class MusicChatConsoleRunner implements CommandLineRunner {
   ```

## Customization

You can easily customize the runner by:

1. **Adding new action types** to the `ActionType` enum
2. **Modifying sample data** arrays
3. **Adjusting timing and concurrency** parameters
4. **Adding new service interactions**
5. **Customizing console output format**

## Integration with CI/CD

The ConsoleRunner is perfect for:
- **Load testing** during development
- **Integration testing** in CI pipelines
- **Demo environments** for showcasing functionality
- **Performance benchmarking**

## Troubleshooting

Common issues and solutions:

1. **No users available**: Ensure DataSeeder has run and created users
2. **Authentication failures**: Check that UserDetailsService is properly configured
3. **File upload errors**: Verify upload directory permissions
4. **Database connection issues**: Ensure database is accessible and properly configured

## Example Output

When running the application, you'll see output similar to:

```
2024-01-15 10:30:15.123  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : 🎵 MusicChat Multi-User Simulation Console Runner Started
2024-01-15 10:30:15.124  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : Simulating 10 users performing 5 actions each
2024-01-15 10:30:15.125  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : Using 5 concurrent threads for simulation
2024-01-15 10:30:15.127  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : Selected 10 users for simulation: [admin, alice, bob, charlie, diana, eve, frank, grace, henry, iris]
2024-01-15 10:30:17.234  INFO --- [pool-2-thread-1] e.c.c.m.r.MusicChatConsoleRunner : 👤 Starting simulation for user: alice
2024-01-15 10:30:17.235  INFO --- [pool-2-thread-1] e.c.c.m.r.MusicChatConsoleRunner : 💬 alice sent message: "Hey everyone! What's your favorite genre?"
2024-01-15 10:30:17.456  INFO --- [pool-2-thread-1] e.c.c.m.r.MusicChatConsoleRunner : 🎵 alice uploaded track: "Bohemian Rhapsody" by Queen
...
2024-01-15 10:30:25.789  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : ✅ Multi-user simulation completed successfully!
2024-01-15 10:30:25.790  INFO --- [main] e.c.c.m.r.MusicChatConsoleRunner : Check the console output above to see all simulated actions.
```
