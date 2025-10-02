package edu.cqu.coit13235.musicchat.repository;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ChatMessage entity.
 * Provides data access methods for chat messages.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all messages ordered by timestamp (oldest first).
     * @return List of all chat messages in chronological order
     */
    @Query("SELECT cm FROM ChatMessage cm ORDER BY cm.timestamp ASC")
    List<ChatMessage> findAllOrderByTimestamp();
    
    /**
     * Find the last N messages ordered by timestamp (oldest first).
     * @param limit Maximum number of messages to return
     * @return List of the last N chat messages in chronological order
     */
    @Query("SELECT cm FROM ChatMessage cm ORDER BY cm.timestamp ASC")
    List<ChatMessage> findLastMessages(int limit);
    
    /**
     * Find messages by sender.
     * @param sender The sender's username
     * @return List of messages from the specified sender
     */
    List<ChatMessage> findBySenderOrderByTimestampAsc(String sender);
    
    /**
     * Count total number of messages.
     * @return Total count of messages
     */
    @Override
    long count();
}
