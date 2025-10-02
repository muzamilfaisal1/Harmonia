package edu.cqu.coit13235.musicchat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import edu.cqu.coit13235.musicchat.domain.PlaylistTrack;

/**
 * Repository interface for PlaylistTrack entity.
 * Provides data access methods for playlist-track relationships.
 */
@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    
    /**
     * Find all tracks in a playlist ordered by position.
     * @param playlistId The playlist ID
     * @return List of playlist tracks ordered by position
     */
    @Query("SELECT pt FROM PlaylistTrack pt WHERE pt.playlist.id = :playlistId ORDER BY pt.position ASC")
    List<PlaylistTrack> findByPlaylistIdOrderByPosition(Long playlistId);
    
    /**
     * Find playlist tracks by playlist ID.
     * @param playlistId The playlist ID
     * @return List of playlist tracks for the specified playlist
     */
    List<PlaylistTrack> findByPlaylistId(Long playlistId);
    
    /**
     * Find playlist tracks by track ID.
     * @param trackId The track ID
     * @return List of playlist tracks containing the specified track
     */
    List<PlaylistTrack> findByTrackId(Long trackId);
    
    /**
     * Find playlist track by playlist ID and track ID.
     * @param playlistId The playlist ID
     * @param trackId The track ID
     * @return List of playlist tracks matching both criteria
     */
    List<PlaylistTrack> findByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    
    /**
     * Delete all tracks from a playlist.
     * @param playlistId The playlist ID
     */
    void deleteByPlaylistId(Long playlistId);
    
    /**
     * Count tracks in a playlist.
     * @param playlistId The playlist ID
     * @return Number of tracks in the specified playlist
     */
    long countByPlaylistId(Long playlistId);
    
    /**
     * Find the maximum position in a playlist.
     * @param playlistId The playlist ID
     * @return The maximum position value, or null if no tracks exist
     */
    @Query("SELECT MAX(pt.position) FROM PlaylistTrack pt WHERE pt.playlist.id = :playlistId")
    Integer findMaxPositionByPlaylistId(Long playlistId);
}
