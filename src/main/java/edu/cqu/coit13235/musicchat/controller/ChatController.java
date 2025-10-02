package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.dto.ChatMessageRequest;
import edu.cqu.coit13235.musicchat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for chat functionality.
 * Provides endpoints for sending and retrieving chat messages.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class ChatController {
    
    private final ChatService chatService;
    
    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Send a new chat message.
     * POST /api/chat/messages
     * 
     * @param request JSON object containing only the message content
     * @param principal The authenticated user principal
     * @return ResponseEntity containing the created message
     */
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageRequest request, Principal principal) {
        try {
            // Check if user is authenticated
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Authentication required"));
            }
            
            // Extract username from authentication
            String username = principal.getName();
            String content = request.getContent();
            
            // Create and save the message with the authenticated user's username
            ChatMessage message = chatService.sendMessage(username, content);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get chat messages.
     * GET /api/chat/messages
     * 
     * @param limit Optional query parameter to limit the number of messages returned
     * @return ResponseEntity containing the list of messages
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(@RequestParam(required = false) Integer limit) {
        try {
            List<ChatMessage> messages;
            if (limit != null) {
                // Validate limit parameter - this will throw IllegalArgumentException if limit <= 0
                messages = chatService.getConversation(limit);
            } else {
                messages = chatService.getConversation();
            }
            
            return ResponseEntity.ok(messages);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid limit parameter", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get a specific message by ID.
     * GET /api/chat/messages/{id}
     * 
     * @param id The message ID
     * @return ResponseEntity containing the message or 404 if not found
     */
    @GetMapping("/messages/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        try {
            return chatService.getMessageById(id)
                .map(message -> ResponseEntity.ok(message))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get messages from a specific sender.
     * GET /api/chat/messages/sender/{sender}
     * 
     * @param sender The sender's username
     * @return ResponseEntity containing the list of messages from the sender
     */
    @GetMapping("/messages/sender/{sender}")
    public ResponseEntity<?> getMessagesBySender(@PathVariable String sender) {
        try {
            List<ChatMessage> messages = chatService.getMessagesBySender(sender);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid sender parameter", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get message count.
     * GET /api/chat/messages/count
     * 
     * @return ResponseEntity containing the total number of messages
     */
    @GetMapping("/messages/count")
    public ResponseEntity<?> getMessageCount() {
        try {
            long count = chatService.getMessageCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
