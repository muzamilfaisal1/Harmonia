package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for chat functionality.
 * Handles business logic for sending and retrieving chat messages.
 */
@Service
@Transactional
public class ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    
    @Autowired
    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }
    
    /**
     * Send a new chat message.
     * @param sender The username of the sender
     * @param text The message text
     * @return The saved ChatMessage with generated ID and timestamp
     * @throws IllegalArgumentException if sender or text is null or empty
     */
    public ChatMessage sendMessage(String sender, String text) {
        if (sender == null || sender.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        ChatMessage message = new ChatMessage(sender.trim(), text.trim());
        return chatMessageRepository.save(message);
    }
    
    /**
     * Send a new chat message with a reference to a previous message.
     * @param sender The username of the sender
     * @param text The message text
     * @param previousId The ID of the previous message (for threading)
     * @return The saved ChatMessage with generated ID and timestamp
     * @throws IllegalArgumentException if sender or text is null or empty
     */
    public ChatMessage sendMessage(String sender, String text, Long previousId) {
        if (sender == null || sender.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        ChatMessage message = new ChatMessage(sender.trim(), text.trim(), previousId);
        return chatMessageRepository.save(message);
    }
    
    /**
     * Get the entire conversation history.
     * @return List of all chat messages in chronological order
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation() {
        return chatMessageRepository.findAllOrderByTimestamp();
    }
    
    /**
     * Get the last N messages from the conversation.
     * @param limit Maximum number of messages to return
     * @return List of the last N chat messages in chronological order
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        List<ChatMessage> allMessages = chatMessageRepository.findAllOrderByTimestamp();
        if (allMessages.size() <= limit) {
            return allMessages;
        }
        
        return allMessages.subList(Math.max(0, allMessages.size() - limit), allMessages.size());
    }
    
    /**
     * Get a specific message by ID.
     * @param id The message ID
     * @return Optional containing the message if found
     */
    @Transactional(readOnly = true)
    public Optional<ChatMessage> getMessageById(Long id) {
        return chatMessageRepository.findById(id);
    }
    
    /**
     * Get messages from a specific sender.
     * @param sender The sender's username
     * @return List of messages from the specified sender
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesBySender(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender cannot be null or empty");
        }
        return chatMessageRepository.findBySenderOrderByTimestampAsc(sender.trim());
    }
    
    /**
     * Get the total number of messages in the conversation.
     * @return Total count of messages
     */
    @Transactional(readOnly = true)
    public long getMessageCount() {
        return chatMessageRepository.count();
    }
}
