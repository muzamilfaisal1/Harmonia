package edu.cqu.coit13235.musicchat.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * PlaylistTrack entity representing the many-to-many relationship between Playlist and AudioTrack.
 * Includes position field for ordering tracks within a playlist.
 */
@Entity
@Table(name = "playlist_tracks")
public class PlaylistTrack {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    @JsonIgnore
    private Playlist playlist;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "track_id", nullable = false)
    private AudioTrack track;
    
    @Column(nullable = false)
    private Integer position;
    
    // Default constructor for JPA
    public PlaylistTrack() {}
    
    public PlaylistTrack(Playlist playlist, AudioTrack track, Integer position) {
        this.playlist = playlist;
        this.track = track;
        this.position = position;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Playlist getPlaylist() {
        return playlist;
    }
    
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }
    
    public AudioTrack getTrack() {
        return track;
    }
    
    public void setTrack(AudioTrack track) {
        this.track = track;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    @Override
    public String toString() {
        return "PlaylistTrack{" +
                "id=" + id +
                ", playlistId=" + (playlist != null ? playlist.getId() : null) +
                ", trackId=" + (track != null ? track.getId() : null) +
                ", position=" + position +
                '}';
    }
}
