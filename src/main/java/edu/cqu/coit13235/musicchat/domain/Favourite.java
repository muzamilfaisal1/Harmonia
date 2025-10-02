package edu.cqu.coit13235.musicchat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Favourite entity representing a user's favourite audio track.
 * Simple toggle functionality for marking tracks as favourites.
 */
@Entity
@Table(name = "favourites", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "track_id"}))
public class Favourite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "track_id", nullable = false)
    private Long trackId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Default constructor for JPA
    protected Favourite() {}
    
    public Favourite(Long userId, Long trackId) {
        this.userId = userId;
        this.trackId = trackId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getTrackId() {
        return trackId;
    }
    
    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Favourite{" +
                "id=" + id +
                ", userId=" + userId +
                ", trackId=" + trackId +
                ", createdAt=" + createdAt +
                '}';
    }
}
