package edu.cqu.coit13235.musicchat.config;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.ChatMessageRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import edu.cqu.coit13235.musicchat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data seeder that runs on application startup.
 * Seeds the H2 database with sample users and chat messages for demonstration.
 * Only runs in non-test profiles to avoid interfering with tests.
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    
    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public DataSeeder(ChatService chatService, ChatMessageRepository chatMessageRepository, 
                     UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Starting data seeding...");
        
        // Check if data already exists
        long userCount = userRepository.count();
        long messageCount = chatMessageRepository.count();
        
        if (userCount > 0 && messageCount > 0) {
            logger.info("Database already contains {} users and {} messages. Skipping data seeding.", userCount, messageCount);
            return;
        }
        
        try {
            // Seed users first
            if (userCount == 0) {
                seedUsers();
            }
            
            // Then seed sample chat messages
            if (messageCount == 0) {
                seedSampleMessages();
            }
            
            logger.info("Data seeding completed successfully. Added {} users and {} messages.", 
                       userRepository.count(), chatMessageRepository.count());
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }
    
    private void seedUsers() {
        logger.info("Seeding users...");
        
        // Create 10 users with different roles
        List<User> users = Arrays.asList(
            new User("admin", "admin@musicchat.com", passwordEncoder.encode("admin123"), "ADMIN"),
            new User("alice", "alice@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("bob", "bob@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("charlie", "charlie@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("diana", "diana@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("eve", "eve@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("frank", "frank@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("grace", "grace@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("henry", "henry@musicchat.com", passwordEncoder.encode("password"), "USER"),
            new User("iris", "iris@musicchat.com", passwordEncoder.encode("password"), "USER")
        );
        
        for (User user : users) {
            try {
                if (!userRepository.existsByUsername(user.getUsername())) {
                    userRepository.save(user);
                    logger.debug("Seeded user: {} with role: {}", user.getUsername(), user.getRole());
                }
            } catch (Exception e) {
                logger.warn("Failed to seed user {}: {}", user.getUsername(), e.getMessage());
            }
        }
        
        logger.info("User seeding completed. Total users: {}", userRepository.count());
    }
    
    private void seedSampleMessages() {
        List<String[]> sampleMessages = Arrays.asList(
            new String[]{"Alice", "Hello everyone! Welcome to MusicChat!"},
            new String[]{"Bob", "Hey Alice! This looks great!"},
            new String[]{"Alice", "Thanks Bob! I'm excited to see what music we'll discover together."},
            new String[]{"Charlie", "Count me in! I love discovering new music."},
            new String[]{"Bob", "What's everyone listening to today?"},
            new String[]{"Alice", "I'm currently obsessed with this new indie band I found."},
            new String[]{"Charlie", "Ooh, which one? I'm always looking for new indie recommendations!"},
            new String[]{"Alice", "They're called 'The Midnight Echoes' - really atmospheric stuff."},
            new String[]{"Bob", "I'll have to check them out! Thanks for the recommendation."},
            new String[]{"Charlie", "Same here! This is exactly why I joined this chat."}
        );
        
        for (String[] messageData : sampleMessages) {
            try {
                chatService.sendMessage(messageData[0], messageData[1]);
                logger.debug("Seeded message: {} - {}", messageData[0], messageData[1]);
            } catch (Exception e) {
                logger.warn("Failed to seed message from {}: {}", messageData[0], e.getMessage());
            }
        }
        
        // Add a few more messages with threading (previousId)
        try {
            // Get the first message to reference
            List<ChatMessage> allMessages = chatMessageRepository.findAllOrderByTimestamp();
            if (!allMessages.isEmpty()) {
                ChatMessage firstMessage = allMessages.get(0);
                
                // Add threaded responses
                chatService.sendMessage("Bob", "I totally agree with your welcome message!", firstMessage.getId());
                chatService.sendMessage("Alice", "Thanks for the support, Bob! 😊", firstMessage.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to seed threaded messages: {}", e.getMessage());
        }
    }
}
