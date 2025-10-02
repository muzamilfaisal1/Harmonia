package edu.cqu.coit13235.musicchat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ChatMessage entity representing a single chat message.
 * Contains fields: id, sender, text, timestamp, and optional previousId for threading.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sender;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "previous_id")
    private Long previousId;
    
    // Default constructor for JPA
    protected ChatMessage() {}
    
    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String sender, String text, Long previousId) {
        this.sender = sender;
        this.text = text;
        this.timestamp = LocalDateTime.now();
        this.previousId = previousId;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Long getPreviousId() {
        return previousId;
    }
    
    public void setPreviousId(Long previousId) {
        this.previousId = previousId;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", sender='" + sender + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", previousId=" + previousId +
                '}';
    }
}
