package edu.cqu.coit13235.musicchat.websocket;

import edu.cqu.coit13235.musicchat.config.WebSocketConfig;
import edu.cqu.coit13235.musicchat.controller.WebSocketChatController;
import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.dto.ChatMessageRequest;
import edu.cqu.coit13235.musicchat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket configuration and controller tests.
 * Verifies WebSocket infrastructure is properly configured without requiring full integration.
 */
@SpringBootTest
@ActiveProfiles("test")
public class WebSocketConfigurationTest {

    @Autowired(required = false)
    private WebSocketConfig webSocketConfig;

    @Autowired(required = false)
    private WebSocketChatController webSocketChatController;

    @Autowired
    private ChatService chatService;

    @Test
    @DisplayName("WebSocket: Configuration bean exists")
    public void testWebSocketConfigExists() {
        assertThat(webSocketConfig).isNotNull();
    }

    @Test
    @DisplayName("WebSocket: Chat controller bean exists")
    public void testWebSocketChatControllerExists() {
        assertThat(webSocketChatController).isNotNull();
    }

    @Test
    @DisplayName("WebSocket: Controller can process messages")
    public void testWebSocketControllerProcessesMessage() {
        // Create a mock message request
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Test WebSocket message");

        // Create mock header accessor
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setUser(new Principal() {
            @Override
            public String getName() {
                return "testuser";
            }
        });

        // Call controller directly
        ChatMessage result = webSocketChatController.sendMessage(request, headerAccessor);

        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("Test WebSocket message");
        assertThat(result.getSender()).isEqualTo("testuser");
        assertThat(result.getId()).isNotNull(); // Message was persisted
    }

    @Test
    @DisplayName("WebSocket: Join notification works")
    public void testUserJoinNotification() {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setUser(new Principal() {
            @Override
            public String getName() {
                return "joinuser";
            }
        });

        ChatMessage joinMessage = webSocketChatController.userJoin(headerAccessor);

        assertThat(joinMessage).isNotNull();
        assertThat(joinMessage.getText()).contains("joinuser");
        assertThat(joinMessage.getText()).contains("joined the chat");
        assertThat(joinMessage.getSender()).isEqualTo("System");
    }
}

