package edu.cqu.coit13235.musicchat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * AudioTrack entity representing an uploaded audio file.
 * Contains metadata about the audio track including file information.
 */
@Entity
@Table(name = "audio_tracks")
public class AudioTrack {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artist;
    
    @Column(nullable = false, unique = true)
    private String filename;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @Column(name = "duration_seconds")
    private Integer duration; // Duration in seconds, optional
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "content_type")
    private String contentType;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Default constructor for JPA
    protected AudioTrack() {}
    
    public AudioTrack(String title, String artist, String filename, String originalFilename, User user) {
        this.title = title;
        this.artist = artist;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.user = user;
        this.uploadedAt = LocalDateTime.now();
    }
    
    public AudioTrack(String title, String artist, String filename, String originalFilename, 
                     Integer duration, Long fileSizeBytes, String contentType, User user) {
        this.title = title;
        this.artist = artist;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.user = user;
        this.uploadedAt = LocalDateTime.now();
        this.duration = duration;
        this.fileSizeBytes = fileSizeBytes;
        this.contentType = contentType;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @Override
    public String toString() {
        return "AudioTrack{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", duration=" + duration +
                ", fileSizeBytes=" + fileSizeBytes +
                ", contentType='" + contentType + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                '}';
    }
}
