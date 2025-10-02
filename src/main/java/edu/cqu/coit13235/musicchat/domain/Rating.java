package edu.cqu.coit13235.musicchat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Rating entity representing a user's rating for an audio track.
 * Supports ratings from 1-5 stars with timestamp tracking.
 */
@Entity
@Table(name = "ratings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "track_id"}))
public class Rating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "track_id", nullable = false)
    private Long trackId;
    
    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue; // 1-5 stars
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected Rating() {}
    
    public Rating(Long userId, Long trackId, Integer ratingValue) {
        this.userId = userId;
        this.trackId = trackId;
        this.ratingValue = ratingValue;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
    
    public Integer getRatingValue() {
        return ratingValue;
    }
    
    public void setRatingValue(Integer ratingValue) {
        this.ratingValue = ratingValue;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "Rating{" +
                "id=" + id +
                ", userId=" + userId +
                ", trackId=" + trackId +
                ", ratingValue=" + ratingValue +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
