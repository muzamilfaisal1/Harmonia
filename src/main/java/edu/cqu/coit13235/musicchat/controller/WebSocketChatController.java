package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.dto.ChatMessageRequest;
import edu.cqu.coit13235.musicchat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat messaging.
 * Handles incoming WebSocket messages and broadcasts them to all connected clients.
 * All messages are also persisted to the database.
 */
@Controller
public class WebSocketChatController {

    private final ChatService chatService;

    @Autowired
    public WebSocketChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Handle incoming WebSocket chat messages.
     * Messages are persisted to the database and broadcast to all connected clients.
     * 
     * @param request The chat message request containing the message content
     * @param headerAccessor Provides access to message headers including user principal
     * @return The saved ChatMessage that will be broadcast to all subscribers
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // Extract username from the WebSocket session
        Principal principal = headerAccessor.getUser();
        String username = (principal != null) ? principal.getName() : "anonymous";
        
        // Save the message to the database
        ChatMessage message = chatService.sendMessage(username, request.getContent());
        
        return message;
    }

    /**
     * Handle user join events.
     * Broadcasts a notification when a user joins the chat.
     * 
     * @param headerAccessor Provides access to message headers including user principal
     * @return A system message indicating the user has joined
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/messages")
    public ChatMessage userJoin(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        String username = (principal != null) ? principal.getName() : "anonymous";
        
        // Create a system message for user join (not persisted)
        ChatMessage joinMessage = new ChatMessage("System", username + " has joined the chat");
        
        return joinMessage;
    }
}

