package edu.cqu.coit13235.musicchat.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.PlaylistTrack;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistRepository;
import edu.cqu.coit13235.musicchat.repository.PlaylistTrackRepository;

/**
 * Service class for playlist management.
 * Handles playlist creation, track management, and retrieval.
 */
@Service
@Transactional
public class PlaylistService {
    
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final AudioTrackRepository audioTrackRepository;
    
    @Autowired
    public PlaylistService(PlaylistRepository playlistRepository,
                          PlaylistTrackRepository playlistTrackRepository,
                          AudioTrackRepository audioTrackRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.audioTrackRepository = audioTrackRepository;
    }
    
    /**
     * Create a new playlist.
     * @param ownerId The ID of the playlist owner
     * @param name The playlist name
     * @param description Optional playlist description
     * @return The created playlist
     */
    public Playlist createPlaylist(Long ownerId, String name, String description) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Playlist name cannot be null or empty");
        }
        
        Playlist playlist = new Playlist(ownerId, name.trim(), description);
        return playlistRepository.save(playlist);
    }
    
    /**
     * Create a new playlist with tracks.
     * @param ownerId The ID of the playlist owner
     * @param name The playlist name
     * @param description Optional playlist description
     * @param trackIds List of track IDs to add to the playlist
     * @return The created playlist with tracks
     */
    public Playlist createPlaylistWithTracks(Long ownerId, String name, String description, List<Long> trackIds) {
        System.out.println("🎵 [DEBUG] createPlaylistWithTracks - Starting with ownerId=" + ownerId + ", name=" + name + ", trackIds=" + trackIds);
        
        Playlist playlist = createPlaylist(ownerId, name, description);
        System.out.println("🎵 [DEBUG] createPlaylistWithTracks - Created base playlist with ID: " + playlist.getId());
        
        if (trackIds != null && !trackIds.isEmpty()) {
            System.out.println("🎵 [DEBUG] createPlaylistWithTracks - Adding tracks to playlist: " + trackIds);
            addTracksToPlaylist(playlist.getId(), trackIds);
            // Refresh the playlist to get updated tracks
            playlist = playlistRepository.findById(playlist.getId()).orElse(playlist);
            System.out.println("🎵 [DEBUG] createPlaylistWithTracks - Refreshed playlist, track count: " + playlist.getTracks().size());
        }
        
        System.out.println("✅ [DEBUG] createPlaylistWithTracks - Successfully created playlist with tracks");
        return playlist;
    }
    
    /**
     * Get playlist by ID.
     * @param id The playlist ID
     * @return Optional containing the playlist if found
     */
    @Transactional(readOnly = true)
    public Optional<Playlist> getPlaylistById(Long id) {
        return playlistRepository.findById(id);
    }
    
    /**
     * Get playlist by ID and owner ID (for security).
     * @param id The playlist ID
     * @param ownerId The owner ID
     * @return Optional containing the playlist if found and owned by the user
     */
    @Transactional(readOnly = true)
    public Optional<Playlist> getPlaylistByIdAndOwner(Long id, Long ownerId) {
        return playlistRepository.findByIdAndOwnerId(id, ownerId);
    }
    
    /**
     * Get all playlists by owner.
     * @param ownerId The owner ID
     * @return List of playlists owned by the user
     */
    @Transactional(readOnly = true)
    public List<Playlist> getPlaylistsByOwner(Long ownerId) {
        return playlistRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }
    
    /**
     * Get all playlists.
     * @return List of all playlists
     */
    @Transactional(readOnly = true)
    public List<Playlist> getAllPlaylists() {
        return playlistRepository.findAllOrderByCreatedAtDesc();
    }
    
    /**
     * Search playlists by name.
     * @param name The name to search for
     * @return List of playlists matching the name
     */
    @Transactional(readOnly = true)
    public List<Playlist> searchPlaylistsByName(String name) {
        return playlistRepository.findByNameContainingIgnoreCase(name);
    }
    
    /**
     * Add tracks to a playlist.
     * @param playlistId The playlist ID
     * @param trackIds List of track IDs to add
     * @return The updated playlist
     */
    public Playlist addTracksToPlaylist(Long playlistId, List<Long> trackIds) {
        System.out.println("🎵 [DEBUG] addTracksToPlaylist - Starting with playlistId=" + playlistId + ", trackIds=" + trackIds);
        
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));
        System.out.println("🎵 [DEBUG] addTracksToPlaylist - Found playlist: " + playlist.getName());
        
        if (trackIds == null || trackIds.isEmpty()) {
            System.out.println("🎵 [DEBUG] addTracksToPlaylist - No tracks to add, returning playlist");
            return playlist;
        }
        
        // Get current maximum position in the playlist
        Integer maxPosition = playlistTrackRepository.findMaxPositionByPlaylistId(playlistId);
        int nextPosition = (maxPosition != null) ? maxPosition + 1 : 0;
        System.out.println("🎵 [DEBUG] addTracksToPlaylist - Current max position: " + maxPosition + ", next position: " + nextPosition);
        
        for (Long trackId : trackIds) {
            System.out.println("🎵 [DEBUG] addTracksToPlaylist - Processing trackId: " + trackId);
            AudioTrack track = audioTrackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Audio track not found with ID: " + trackId));
            System.out.println("🎵 [DEBUG] addTracksToPlaylist - Found track: " + track.getTitle() + " by " + track.getArtist());
            
            // Check if track is already in playlist
            List<PlaylistTrack> existingTracks = playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId);
            if (existingTracks.isEmpty()) {
                System.out.println("🎵 [DEBUG] addTracksToPlaylist - Track not in playlist, adding at position: " + nextPosition);
                PlaylistTrack playlistTrack = new PlaylistTrack();
                playlistTrack.setPlaylist(playlist);
                playlistTrack.setTrack(track);
                playlistTrack.setPosition(nextPosition++);
                playlistTrack = playlistTrackRepository.save(playlistTrack);
                System.out.println("🎵 [DEBUG] addTracksToPlaylist - Saved PlaylistTrack with ID: " + playlistTrack.getId());
                // Add to playlist's tracks list for immediate access
                playlist.getTracks().add(playlistTrack);
            } else {
                System.out.println("🎵 [DEBUG] addTracksToPlaylist - Track already in playlist, skipping");
            }
        }
        
        System.out.println("✅ [DEBUG] addTracksToPlaylist - Successfully added tracks to playlist");
        return playlistRepository.findById(playlistId).orElse(playlist);
    }
    
    /**
     * Remove tracks from a playlist.
     * @param playlistId The playlist ID
     * @param trackIds List of track IDs to remove
     * @return The updated playlist
     */
    public Playlist removeTracksFromPlaylist(Long playlistId, List<Long> trackIds) {
        System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Starting with playlistId=" + playlistId + ", trackIds=" + trackIds);
        
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));
        System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Found playlist: " + playlist.getName());
        
        if (trackIds == null || trackIds.isEmpty()) {
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - No tracks to remove, returning playlist");
            return playlist;
        }
        
        for (Long trackId : trackIds) {
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Processing trackId: " + trackId);
            List<PlaylistTrack> playlistTracks = playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId);
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Found " + playlistTracks.size() + " PlaylistTrack entries to delete");
            playlistTrackRepository.deleteAll(playlistTracks);
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Deleted PlaylistTrack entries for trackId: " + trackId);
        }
        
        // Reorder remaining tracks
        System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Reordering remaining tracks");
        reorderPlaylistTracks(playlistId);
        
        System.out.println("✅ [DEBUG] removeTracksFromPlaylist - Successfully removed tracks from playlist");
        return playlistRepository.findById(playlistId).orElse(playlist);
    }
    
    /**
     * Get tracks in a playlist ordered by position.
     * @param playlistId The playlist ID
     * @return List of playlist tracks ordered by position
     */
    @Transactional(readOnly = true)
    public List<PlaylistTrack> getPlaylistTracks(Long playlistId) {
        return playlistTrackRepository.findByPlaylistIdOrderByPosition(playlistId);
    }
    
    /**
     * Update playlist information.
     * @param playlistId The playlist ID
     * @param name New playlist name
     * @param description New playlist description
     * @return The updated playlist
     */
    public Playlist updatePlaylist(Long playlistId, String name, String description) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));
        
        if (name != null && !name.trim().isEmpty()) {
            playlist.setName(name.trim());
        }
        
        playlist.setDescription(description);
        
        return playlistRepository.save(playlist);
    }
    
    /**
     * Delete a playlist and all its tracks.
     * @param playlistId The playlist ID
     * @return true if deleted successfully, false if not found
     */
    public boolean deletePlaylist(Long playlistId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        if (playlistOpt.isPresent()) {
            // Delete all playlist tracks first
            playlistTrackRepository.deleteByPlaylistId(playlistId);
            
            // Delete the playlist
            playlistRepository.deleteById(playlistId);
            return true;
        }
        return false;
    }
    
    /**
     * Reorder tracks in a playlist to maintain sequential positions.
     * @param playlistId The playlist ID
     */
    private void reorderPlaylistTracks(Long playlistId) {
        List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistIdOrderByPosition(playlistId);
        
        for (int i = 0; i < tracks.size(); i++) {
            PlaylistTrack track = tracks.get(i);
            track.setPosition(i);
            playlistTrackRepository.save(track);
        }
    }
    
    /**
     * Get total number of playlists.
     * @return Total count of playlists
     */
    @Transactional(readOnly = true)
    public long getPlaylistCount() {
        return playlistRepository.count();
    }
    
    /**
     * Get number of playlists by owner.
     * @param ownerId The owner ID
     * @return Number of playlists owned by the user
     */
    @Transactional(readOnly = true)
    public long getPlaylistCountByOwner(Long ownerId) {
        return playlistRepository.countByOwnerId(ownerId);
    }
}
