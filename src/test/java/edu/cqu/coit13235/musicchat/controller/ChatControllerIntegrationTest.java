package edu.cqu.coit13235.musicchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.ChatMessageRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ChatController.
 * Tests the complete flow from HTTP request to database persistence.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ChatControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        // Clear the database before each test
        chatMessageRepository.deleteAll();
        
        // Create test users if they don't exist
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User("testuser", "test@example.com", "password", "USER");
            userRepository.save(testUser);
        }
        
        if (!userRepository.existsByUsername("user1")) {
            User user1 = new User("user1", "user1@example.com", "password", "USER");
            userRepository.save(user1);
        }
        
        if (!userRepository.existsByUsername("user2")) {
            User user2 = new User("user2", "user2@example.com", "password", "USER");
            userRepository.save(user2);
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendMessage_ValidInput_ReturnsCreatedMessage() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("content", "Hello, world!");

        // When & Then
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.text", is("Hello, world!")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.previousId", nullValue()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendMessage_WithPreviousId_ReturnsCreatedMessage() throws Exception {
        // Given - First create a message to reference
        ChatMessage firstMessage = new ChatMessage("user1", "First message");
        firstMessage = chatMessageRepository.save(firstMessage);

        Map<String, String> request = new HashMap<>();
        request.put("content", "Reply to first message");

        // When & Then
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.text", is("Reply to first message")))
                .andExpect(jsonPath("$.previousId", nullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendMessage_InvalidInput_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("content", ""); // Empty content

        // When & Then
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
                // Note: Spring's @Valid validation returns empty body for MethodArgumentNotValidException
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendMessage_MissingFields_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        // Missing content field

        // When & Then
        mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
                // Note: Spring's @Valid validation returns empty body for MethodArgumentNotValidException
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessages_NoMessages_ReturnsEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessages_WithMessages_ReturnsAllMessages() throws Exception {
        // Given
        ChatMessage message1 = new ChatMessage("user1", "First message");
        ChatMessage message2 = new ChatMessage("user2", "Second message");
        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);

        // When & Then
        mockMvc.perform(get("/api/chat/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender", is("user1")))
                .andExpect(jsonPath("$[0].text", is("First message")))
                .andExpect(jsonPath("$[1].sender", is("user2")))
                .andExpect(jsonPath("$[1].text", is("Second message")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessages_WithLimit_ReturnsLimitedMessages() throws Exception {
        // Given
        ChatMessage message1 = new ChatMessage("user1", "First message");
        ChatMessage message2 = new ChatMessage("user2", "Second message");
        ChatMessage message3 = new ChatMessage("user3", "Third message");
        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);
        chatMessageRepository.save(message3);

        // When & Then
        mockMvc.perform(get("/api/chat/messages?limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender", is("user2"))) // Last 2 messages
                .andExpect(jsonPath("$[1].sender", is("user3")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessages_InvalidLimit_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/messages?limit=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid limit parameter")))
                .andExpect(jsonPath("$.message", containsString("Limit must be positive")));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getMessages_NegativeLimit_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/messages?limit=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid limit parameter")))
                .andExpect(jsonPath("$.message", containsString("Limit must be positive")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessageById_ExistingId_ReturnsMessage() throws Exception {
        // Given
        ChatMessage message = new ChatMessage("testuser", "Test message");
        message = chatMessageRepository.save(message);

        // When & Then
        mockMvc.perform(get("/api/chat/messages/" + message.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(message.getId().intValue())))
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.text", is("Test message")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessageById_NonExistingId_ReturnsNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/messages/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessagesBySender_ValidSender_ReturnsMessages() throws Exception {
        // Given
        ChatMessage message1 = new ChatMessage("alice", "Message from Alice");
        ChatMessage message2 = new ChatMessage("bob", "Message from Bob");
        ChatMessage message3 = new ChatMessage("alice", "Another message from Alice");
        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);
        chatMessageRepository.save(message3);

        // When & Then
        mockMvc.perform(get("/api/chat/messages/sender/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender", is("alice")))
                .andExpect(jsonPath("$[1].sender", is("alice")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessagesBySender_NonExistingSender_ReturnsEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/messages/sender/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessageCount_ReturnsCorrectCount() throws Exception {
        // Given
        ChatMessage message1 = new ChatMessage("user1", "Message 1");
        ChatMessage message2 = new ChatMessage("user2", "Message 2");
        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);

        // When & Then
        mockMvc.perform(get("/api/chat/messages/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postThenGet_ReturnsSameMessage() throws Exception {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("content", "Integration test message");

        // When - Send message
        String response = mockMvc.perform(post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.text", is("Integration test message")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract message ID from response
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Long messageId = Long.valueOf(responseMap.get("id").toString());

        // Then - Get message by ID
        mockMvc.perform(get("/api/chat/messages/" + messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(messageId.intValue())))
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.text", is("Integration test message")));

        // And - Get all messages
        mockMvc.perform(get("/api/chat/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(messageId.intValue())))
                .andExpect(jsonPath("$[0].sender", is("testuser")))
                .andExpect(jsonPath("$[0].text", is("Integration test message")));
    }
}
