package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 * Tests business logic without database dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @InjectMocks
    private ChatService chatService;
    
    private ChatMessage sampleMessage;
    
    @BeforeEach
    void setUp() {
        sampleMessage = new ChatMessage("testuser", "Hello, world!");
        sampleMessage.setId(1L);
        sampleMessage.setTimestamp(LocalDateTime.now());
    }
    
    @Test
    void sendMessage_ValidInput_ReturnsSavedMessage() {
        // Given
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(sampleMessage);
        
        // When
        ChatMessage result = chatService.sendMessage("testuser", "Hello, world!");
        
        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getSender());
        assertEquals("Hello, world!", result.getText());
        assertNotNull(result.getTimestamp());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_WithPreviousId_ReturnsSavedMessage() {
        // Given
        ChatMessage replyMessage = new ChatMessage("testuser", "Reply message", 1L);
        replyMessage.setId(2L);
        replyMessage.setTimestamp(LocalDateTime.now());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(replyMessage);
        
        // When
        ChatMessage result = chatService.sendMessage("testuser", "Reply message", 1L);
        
        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getSender());
        assertEquals("Reply message", result.getText());
        assertEquals(1L, result.getPreviousId());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_NullSender_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> chatService.sendMessage(null, "Hello")
        );
        assertEquals("Sender cannot be null or empty", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_EmptySender_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> chatService.sendMessage("", "Hello")
        );
        assertEquals("Sender cannot be null or empty", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_NullText_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> chatService.sendMessage("testuser", null)
        );
        assertEquals("Text cannot be null or empty", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_EmptyText_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> chatService.sendMessage("testuser", "")
        );
        assertEquals("Text cannot be null or empty", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
    
    @Test
    void getConversation_ReturnsAllMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(sampleMessage);
        when(chatMessageRepository.findAllOrderByTimestamp()).thenReturn(messages);
        
        // When
        List<ChatMessage> result = chatService.getConversation();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleMessage, result.get(0));
        verify(chatMessageRepository).findAllOrderByTimestamp();
    }
    
    @Test
    void getConversation_WithLimit_ReturnsLimitedMessages() {
        // Given
        ChatMessage message1 = new ChatMessage("user1", "First message");
        ChatMessage message2 = new ChatMessage("user2", "Second message");
        ChatMessage message3 = new ChatMessage("user3", "Third message");
        List<ChatMessage> allMessages = Arrays.asList(message1, message2, message3);
        when(chatMessageRepository.findAllOrderByTimestamp()).thenReturn(allMessages);
        
        // When
        List<ChatMessage> result = chatService.getConversation(2);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(message2, result.get(0)); // Last 2 messages
        assertEquals(message3, result.get(1));
        verify(chatMessageRepository).findAllOrderByTimestamp();
    }
    
    @Test
    void getConversation_WithLimitLargerThanTotal_ReturnsAllMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(sampleMessage);
        when(chatMessageRepository.findAllOrderByTimestamp()).thenReturn(messages);
        
        // When
        List<ChatMessage> result = chatService.getConversation(5);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleMessage, result.get(0));
    }
    
    @Test
    void getConversation_InvalidLimit_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> chatService.getConversation(0));
        assertThrows(IllegalArgumentException.class, () -> chatService.getConversation(-1));
    }
    
    @Test
    void getMessageById_ExistingId_ReturnsMessage() {
        // Given
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(sampleMessage));
        
        // When
        Optional<ChatMessage> result = chatService.getMessageById(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(sampleMessage, result.get());
        verify(chatMessageRepository).findById(1L);
    }
    
    @Test
    void getMessageById_NonExistingId_ReturnsEmpty() {
        // Given
        when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When
        Optional<ChatMessage> result = chatService.getMessageById(999L);
        
        // Then
        assertFalse(result.isPresent());
        verify(chatMessageRepository).findById(999L);
    }
    
    @Test
    void getMessagesBySender_ValidSender_ReturnsMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(sampleMessage);
        when(chatMessageRepository.findBySenderOrderByTimestampAsc("testuser")).thenReturn(messages);
        
        // When
        List<ChatMessage> result = chatService.getMessagesBySender("testuser");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleMessage, result.get(0));
        verify(chatMessageRepository).findBySenderOrderByTimestampAsc("testuser");
    }
    
    @Test
    void getMessagesBySender_NullSender_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> chatService.getMessagesBySender(null));
        assertThrows(IllegalArgumentException.class, () -> chatService.getMessagesBySender(""));
    }
    
    @Test
    void getMessageCount_ReturnsCount() {
        // Given
        when(chatMessageRepository.count()).thenReturn(5L);
        
        // When
        long result = chatService.getMessageCount();
        
        // Then
        assertEquals(5L, result);
        verify(chatMessageRepository).count();
    }
}
