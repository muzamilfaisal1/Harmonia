package edu.cqu.coit13235.musicchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for chat message requests.
 * Only contains the message content - username is extracted from authentication.
 */
public class ChatMessageRequest {
    
    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;
    
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}
