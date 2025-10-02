package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.domain.Playlist;
import edu.cqu.coit13235.musicchat.domain.PlaylistTrack;
import edu.cqu.coit13235.musicchat.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for playlist operations.
 * Provides endpoints for creating, retrieving, and managing playlists.
 */
@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class PlaylistController {
    
    private final PlaylistService playlistService;
    
    @Autowired
    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
    
    /**
     * Create a new playlist.
     * POST /api/playlists
     * 
     * @param request JSON object containing ownerId, name, description, and trackIds
     * @return ResponseEntity containing the created playlist
     */
    @PostMapping
    public ResponseEntity<?> createPlaylist(@RequestBody Map<String, Object> request) {
        System.out.println("🎵 [DEBUG] createPlaylist - Starting with request: " + request);
        try {
            Long ownerId = Long.valueOf(request.get("ownerId").toString());
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<Object> trackIdsRaw = (List<Object>) request.get("trackIds");
            List<Long> trackIds = trackIdsRaw != null ? trackIdsRaw.stream()
                .map(item -> Long.valueOf(item.toString()))
                .collect(java.util.stream.Collectors.toList()) : null;
            
            System.out.println("🎵 [DEBUG] createPlaylist - Parsed data: ownerId=" + ownerId + ", name=" + name + ", description=" + description + ", trackIds=" + trackIds);
            
            if (name == null || name.trim().isEmpty()) {
                System.out.println("❌ [DEBUG] createPlaylist - Validation failed: name is null or empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input", "message", "Playlist name is required"));
            }
            
            Playlist playlist;
            if (trackIds != null && !trackIds.isEmpty()) {
                System.out.println("🎵 [DEBUG] createPlaylist - Creating playlist with tracks: " + trackIds);
                playlist = playlistService.createPlaylistWithTracks(ownerId, name.trim(), description, trackIds);
            } else {
                System.out.println("🎵 [DEBUG] createPlaylist - Creating playlist without tracks");
                playlist = playlistService.createPlaylist(ownerId, name.trim(), description);
            }
            
            System.out.println("✅ [DEBUG] createPlaylist - Successfully created playlist with ID: " + playlist.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
            
        } catch (NumberFormatException e) {
            System.out.println("❌ [DEBUG] createPlaylist - NumberFormatException: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid ownerId format", "message", "ownerId must be a valid number"));
        } catch (IllegalArgumentException e) {
            System.out.println("❌ [DEBUG] createPlaylist - IllegalArgumentException: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ [DEBUG] createPlaylist - Unexpected Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get playlist by ID.
     * GET /api/playlists/{id}
     * 
     * @param id The playlist ID
     * @return ResponseEntity containing the playlist or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaylistById(@PathVariable Long id) {
        try {
            Optional<Playlist> playlist = playlistService.getPlaylistById(id);
            return playlist.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get playlist by ID and owner ID (for security).
     * GET /api/playlists/{id}/owner/{ownerId}
     * 
     * @param id The playlist ID
     * @param ownerId The owner ID
     * @return ResponseEntity containing the playlist or 404 if not found
     */
    @GetMapping("/{id}/owner/{ownerId}")
    public ResponseEntity<?> getPlaylistByIdAndOwner(@PathVariable Long id, @PathVariable Long ownerId) {
        try {
            Optional<Playlist> playlist = playlistService.getPlaylistByIdAndOwner(id, ownerId);
            return playlist.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all playlists by owner.
     * GET /api/playlists/owner/{ownerId}
     * 
     * @param ownerId The owner ID
     * @return ResponseEntity containing the list of playlists
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> getPlaylistsByOwner(@PathVariable Long ownerId) {
        try {
            List<Playlist> playlists = playlistService.getPlaylistsByOwner(ownerId);
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all playlists.
     * GET /api/playlists
     * 
     * @return ResponseEntity containing the list of all playlists
     */
    @GetMapping
    public ResponseEntity<?> getAllPlaylists() {
        try {
            List<Playlist> playlists = playlistService.getAllPlaylists();
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Search playlists by name.
     * GET /api/playlists/search?q={query}
     * 
     * @param query The search query
     * @return ResponseEntity containing the list of matching playlists
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPlaylistsByName(@RequestParam("q") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid query", "message", "Search query cannot be empty"));
            }
            
            List<Playlist> playlists = playlistService.searchPlaylistsByName(query.trim());
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get tracks in a playlist.
     * GET /api/playlists/{id}/tracks
     * 
     * @param id The playlist ID
     * @return ResponseEntity containing the list of playlist tracks
     */
    @GetMapping("/{id}/tracks")
    public ResponseEntity<?> getPlaylistTracks(@PathVariable Long id) {
        try {
            List<PlaylistTrack> tracks = playlistService.getPlaylistTracks(id);
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Add tracks to a playlist.
     * POST /api/playlists/{id}/tracks
     * 
     * @param id The playlist ID
     * @param request JSON object containing trackIds array
     * @return ResponseEntity containing the updated playlist
     */
    @PostMapping("/{id}/tracks")
    public ResponseEntity<?> addTracksToPlaylist(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        System.out.println("🎵 [DEBUG] addTracksToPlaylist - Starting with playlistId=" + id + ", request=" + request);
        try {
            @SuppressWarnings("unchecked")
            List<Object> trackIdsRaw = (List<Object>) request.get("trackIds");
            List<Long> trackIds = trackIdsRaw != null ? trackIdsRaw.stream()
                .map(item -> Long.valueOf(item.toString()))
                .collect(java.util.stream.Collectors.toList()) : null;
            
            System.out.println("🎵 [DEBUG] addTracksToPlaylist - Parsed trackIds: " + trackIds);
            
            if (trackIds == null || trackIds.isEmpty()) {
                System.out.println("❌ [DEBUG] addTracksToPlaylist - Validation failed: trackIds is " + (trackIds == null ? "null" : "empty"));
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input", "message", "trackIds is required and cannot be empty"));
            }
            
            System.out.println("🎵 [DEBUG] addTracksToPlaylist - Calling service to add tracks to playlist " + id);
            Playlist playlist = playlistService.addTracksToPlaylist(id, trackIds);
            System.out.println("✅ [DEBUG] addTracksToPlaylist - Successfully added tracks to playlist " + playlist.getId());
            return ResponseEntity.ok(playlist);
            
        } catch (IllegalArgumentException e) {
            System.out.println("❌ [DEBUG] addTracksToPlaylist - IllegalArgumentException: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ [DEBUG] addTracksToPlaylist - Unexpected Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Remove tracks from a playlist.
     * DELETE /api/playlists/{id}/tracks
     * 
     * @param id The playlist ID
     * @param request JSON object containing trackIds array
     * @return ResponseEntity containing the updated playlist
     */
    @DeleteMapping("/{id}/tracks")
    public ResponseEntity<?> removeTracksFromPlaylist(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Starting with playlistId=" + id + ", request=" + request);
        try {
            @SuppressWarnings("unchecked")
            List<Object> trackIdsRaw = (List<Object>) request.get("trackIds");
            List<Long> trackIds = trackIdsRaw != null ? trackIdsRaw.stream()
                .map(item -> Long.valueOf(item.toString()))
                .collect(java.util.stream.Collectors.toList()) : null;
            
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Parsed trackIds: " + trackIds);
            
            if (trackIds == null || trackIds.isEmpty()) {
                System.out.println("❌ [DEBUG] removeTracksFromPlaylist - Validation failed: trackIds is " + (trackIds == null ? "null" : "empty"));
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input", "message", "trackIds is required and cannot be empty"));
            }
            
            System.out.println("🎵 [DEBUG] removeTracksFromPlaylist - Calling service to remove tracks from playlist " + id);
            Playlist playlist = playlistService.removeTracksFromPlaylist(id, trackIds);
            System.out.println("✅ [DEBUG] removeTracksFromPlaylist - Successfully removed tracks from playlist " + playlist.getId());
            return ResponseEntity.ok(playlist);
            
        } catch (IllegalArgumentException e) {
            System.out.println("❌ [DEBUG] removeTracksFromPlaylist - IllegalArgumentException: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ [DEBUG] removeTracksFromPlaylist - Unexpected Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Update playlist information.
     * PUT /api/playlists/{id}
     * 
     * @param id The playlist ID
     * @param request JSON object containing name and description
     * @return ResponseEntity containing the updated playlist
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlaylist(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            
            Playlist playlist = playlistService.updatePlaylist(id, name, description);
            return ResponseEntity.ok(playlist);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Delete a playlist.
     * DELETE /api/playlists/{id}
     * 
     * @param id The playlist ID
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id) {
        try {
            boolean deleted = playlistService.deletePlaylist(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Playlist deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Playlist not found", "message", "Playlist with ID " + id + " not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get playlist count.
     * GET /api/playlists/count
     * 
     * @return ResponseEntity containing the total number of playlists
     */
    @GetMapping("/count")
    public ResponseEntity<?> getPlaylistCount() {
        try {
            long count = playlistService.getPlaylistCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get playlist count by owner.
     * GET /api/playlists/count/owner/{ownerId}
     * 
     * @param ownerId The owner ID
     * @return ResponseEntity containing the number of playlists owned by the user
     */
    @GetMapping("/count/owner/{ownerId}")
    public ResponseEntity<?> getPlaylistCountByOwner(@PathVariable Long ownerId) {
        try {
            long count = playlistService.getPlaylistCountByOwner(ownerId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
