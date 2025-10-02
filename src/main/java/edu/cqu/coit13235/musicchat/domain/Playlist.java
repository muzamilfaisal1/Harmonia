package edu.cqu.coit13235.musicchat.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Playlist entity representing a collection of audio tracks.
 * Contains playlist metadata and references to tracks through PlaylistTrack join table.
 */
@Entity
@Table(name = "playlists")
public class Playlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "description")
    private String description;
    
    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<edu.cqu.coit13235.musicchat.domain.PlaylistTrack> tracks = new ArrayList<>();
    
    // Default constructor for JPA
    protected Playlist() {}
    
    public Playlist(Long ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }
    
    public Playlist(Long ownerId, String name, String description) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<edu.cqu.coit13235.musicchat.domain.PlaylistTrack> getTracks() {
        return tracks;
    }
    
    public void setTracks(List<edu.cqu.coit13235.musicchat.domain.PlaylistTrack> tracks) {
        this.tracks = tracks;
    }
    
    /**
     * Add a track to the playlist at the specified position.
     * @param track The audio track to add
     * @param position The position in the playlist (0-based)
     */
    public void addTrack(AudioTrack track, int position) {
        edu.cqu.coit13235.musicchat.domain.PlaylistTrack playlistTrack = new edu.cqu.coit13235.musicchat.domain.PlaylistTrack(this, track, position);
        tracks.add(playlistTrack);
    }
    
    /**
     * Get the number of tracks in the playlist.
     * @return The number of tracks
     */
    public int getTrackCount() {
        return tracks.size();
    }
    
    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", description='" + description + '\'' +
                ", trackCount=" + getTrackCount() +
                '}';
    }
}
