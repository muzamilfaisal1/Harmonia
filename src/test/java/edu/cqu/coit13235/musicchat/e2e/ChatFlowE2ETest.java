package edu.cqu.coit13235.musicchat.e2e;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.ChatMessageRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End tests for Chat Flow.
 * Tests the complete workflow: send message → persist to H2 → retrieve via GET.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChatFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    public void setUp() {
        chatMessageRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("E2E: Send message via API → verify persistence in H2 → retrieve via GET")
    public void testChatFlowEndToEnd() throws Exception {
        // Step 1: Send a message via POST API
        String messageContent = "Hello, this is a test message!";
        String jsonRequest = String.format("{\"content\":\"%s\"}", messageContent);

        mockMvc.perform(post("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sender").value(testUser.getUsername()))
                .andExpect(jsonPath("$.text").value(messageContent))
                .andExpect(jsonPath("$.timestamp").exists());

        // Step 2: Verify persistence in H2 database
        List<ChatMessage> messages = chatMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSender()).isEqualTo(testUser.getUsername());
        assertThat(messages.get(0).getText()).isEqualTo(messageContent);

        // Step 3: Retrieve message via GET API
        mockMvc.perform(get("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sender").value(testUser.getUsername()))
                .andExpect(jsonPath("$[0].text").value(messageContent));
    }

    @Test
    @DisplayName("E2E: Conversation history is ordered correctly by timestamp")
    public void testConversationOrderingEndToEnd() throws Exception {
        // Send multiple messages
        String[] messages = {"First message", "Second message", "Third message"};

        for (String content : messages) {
            String jsonRequest = String.format("{\"content\":\"%s\"}", content);
            mockMvc.perform(post("/api/chat/messages")
                    .with(user(testUser.getUsername()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isCreated());

            // Small delay to ensure distinct timestamps
            Thread.sleep(10);
        }

        // Verify messages are stored in H2
        List<ChatMessage> storedMessages = chatMessageRepository.findAll();
        assertThat(storedMessages).hasSize(3);

        // Retrieve and verify ordering
        mockMvc.perform(get("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].text").value("First message"))
                .andExpect(jsonPath("$[1].text").value("Second message"))
                .andExpect(jsonPath("$[2].text").value("Third message"));
    }

    @Test
    @DisplayName("E2E: Multiple users can send messages independently")
    public void testMultipleUsersChat() throws Exception {
        // Create second user
        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password"));
        user2 = userRepository.save(user2);

        // User 1 sends a message
        mockMvc.perform(post("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Message from user 1\"}"))
                .andExpect(status().isCreated());

        // User 2 sends a message
        mockMvc.perform(post("/api/chat/messages")
                .with(user(user2.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Message from user 2\"}"))
                .andExpect(status().isCreated());

        // Verify both messages are stored
        List<ChatMessage> messages = chatMessageRepository.findAll();
        assertThat(messages).hasSize(2);

        // Retrieve all messages
        mockMvc.perform(get("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender").value(testUser.getUsername()))
                .andExpect(jsonPath("$[1].sender").value(user2.getUsername()));
    }

    @Test
    @DisplayName("E2E: Retrieve message by ID")
    public void testRetrieveMessageById() throws Exception {
        // Send a message
        String messageContent = "Test message for ID retrieval";
        String response = mockMvc.perform(post("/api/chat/messages")
                .with(user(testUser.getUsername()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"content\":\"%s\"}", messageContent)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract ID from response (simple approach)
        Long messageId = chatMessageRepository.findAll().get(0).getId();

        // Retrieve message by ID
        mockMvc.perform(get("/api/chat/messages/" + messageId)
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId))
                .andExpect(jsonPath("$.text").value(messageContent));
    }

    @Test
    @DisplayName("E2E: Retrieve messages with limit parameter")
    public void testRetrieveMessagesWithLimit() throws Exception {
        // Send 5 messages
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/chat/messages")
                    .with(user(testUser.getUsername()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"content\":\"Message %d\"}", i)))
                    .andExpect(status().isCreated());
            Thread.sleep(10);
        }

        // Retrieve only last 3 messages
        mockMvc.perform(get("/api/chat/messages?limit=3")
                .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].text").value("Message 3"))
                .andExpect(jsonPath("$[1].text").value("Message 4"))
                .andExpect(jsonPath("$[2].text").value("Message 5"));
    }
}

