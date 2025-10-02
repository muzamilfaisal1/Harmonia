package edu.cqu.coit13235.musicchat.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.Favourite;
import edu.cqu.coit13235.musicchat.domain.Rating;
import edu.cqu.coit13235.musicchat.service.AudioService;
import edu.cqu.coit13235.musicchat.service.RatingService;
import edu.cqu.coit13235.musicchat.service.FavouriteService;
import edu.cqu.coit13235.musicchat.service.ExternalMusicService;

/**
 * REST controller for audio file operations.
 * Provides endpoints for uploading, retrieving, and managing audio tracks.
 */
@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class AudioController {
    
    private final AudioService audioService;
    private final RatingService ratingService;
    private final FavouriteService favouriteService;
    private final ExternalMusicService externalMusicService;
    
    @Autowired
    public AudioController(AudioService audioService, RatingService ratingService, 
                          FavouriteService favouriteService, ExternalMusicService externalMusicService) {
        this.audioService = audioService;
        this.ratingService = ratingService;
        this.favouriteService = favouriteService;
        this.externalMusicService = externalMusicService;
    }
    
    /**
     * Get the current authenticated user ID.
     * For demo purposes, returns 1 if user is authenticated.
     * 
     * @return the current user ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            // For demo purposes, return 1 for any authenticated user
            // In a real application, you would look up the user ID from the database
            return 1L;
        }
        return null;
    }
    
    /**
     * Upload an audio file.
     * POST /api/audio/upload
     * 
     * @param file The audio file to upload
     * @param title The title of the audio track
     * @param artist The artist name
     * @return ResponseEntity containing the created AudioTrack
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file,
                                       @RequestParam("title") String title,
                                       @RequestParam("artist") String artist) {
        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Title is required", "message", "Title cannot be null or empty"));
            }
            
            if (artist == null || artist.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Artist is required", "message", "Artist cannot be null or empty"));
            }
            
            AudioTrack audioTrack = audioService.uploadAudio(file, title.trim(), artist.trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(audioTrack);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "File upload failed", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all audio tracks.
     * GET /api/audio/tracks
     * 
     * @return ResponseEntity containing the list of audio tracks
     */
    @GetMapping("/tracks")
    public ResponseEntity<?> getAllTracks() {
        try {
            List<AudioTrack> tracks = audioService.getAllTracks();
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/audio/tracks/my
     * 
     * @return ResponseEntity containing tracks uploaded by the current user
     */
    @GetMapping("/tracks/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyTracks() {
        try {
            List<AudioTrack> tracks = audioService.getTracksByCurrentUser();
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve your tracks", "message", e.getMessage()));
        }
    }
    
    /**
     * Get audio track by ID.
     * GET /api/audio/tracks/{id}
     * 
     * @param id The track ID
     * @return ResponseEntity containing the audio track or 404 if not found
     */
    @GetMapping("/tracks/{id}")
    public ResponseEntity<?> getTrackById(@PathVariable Long id) {
        try {
            Optional<AudioTrack> track = audioService.getTrackById(id);
            return track.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Download an audio file.
     * GET /api/audio/download/{id}
     * 
     * @param id The track ID
     * @return ResponseEntity containing the audio file or 404 if not found
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadTrack(@PathVariable Long id) {
        try {
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            AudioTrack track = trackOpt.get();
            
            if (!audioService.fileExists(track)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found", "message", "Audio file does not exist on disk"));
            }
            
            Path filePath = audioService.getFilePath(track);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found", "message", "Audio file resource does not exist"));
            }
            
            String contentType = track.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + track.getOriginalFilename() + "\"")
                .body(resource);
                
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "File path error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Search audio tracks by title.
     * GET /api/audio/search/title?q={query}
     * 
     * @param query The search query
     * @return ResponseEntity containing the list of matching tracks
     */
    @GetMapping("/search/title")
    public ResponseEntity<?> searchTracksByTitle(@RequestParam("q") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid query", "message", "Search query cannot be empty"));
            }
            
            List<AudioTrack> tracks = audioService.searchTracksByTitle(query.trim());
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Search audio tracks by artist.
     * GET /api/audio/search/artist?q={query}
     * 
     * @param query The search query
     * @return ResponseEntity containing the list of matching tracks
     */
    @GetMapping("/search/artist")
    public ResponseEntity<?> searchTracksByArtist(@RequestParam("q") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid query", "message", "Search query cannot be empty"));
            }
            
            List<AudioTrack> tracks = audioService.searchTracksByArtist(query.trim());
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Delete an audio track.
     * DELETE /api/audio/tracks/{id}
     * 
     * @param id The track ID
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/tracks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        try {
            // Check if track exists and user has permission
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
            
            // Check if user owns the track
            AudioTrack track = trackOpt.get();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !track.getUser().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied", "message", "You can only delete your own tracks"));
            }
            
            boolean deleted = audioService.deleteTrack(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Track deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get track count.
     * GET /api/audio/count
     * 
     * @return ResponseEntity containing the total number of tracks
     */
    @GetMapping("/count")
    public ResponseEntity<?> getTrackCount() {
        try {
            long count = audioService.getTrackCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Rate an audio track.
     * POST /api/audio/{id}/rate
     * 
     * @param id The track ID
     * @param requestBody JSON body containing userId and rating
     * @return ResponseEntity containing the rating result
     */
    @PostMapping("/{id}/rate")
    public ResponseEntity<?> rateTrack(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        try {
            // Validate track exists
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
            
            // Extract and validate request parameters
            Object userIdObj = requestBody.get("userId");
            Object ratingObj = requestBody.get("rating");
            
            if (userIdObj == null || ratingObj == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing parameters", "message", "userId and rating are required"));
            }
            
            Long userId;
            Integer rating;
            
            try {
                userId = Long.valueOf(userIdObj.toString());
                rating = Integer.valueOf(ratingObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid parameters", "message", "userId must be a number and rating must be an integer"));
            }
            
            // Rate the track
            Rating savedRating = ratingService.rateTrack(userId, id, rating);
            
            // Get updated statistics
            Double averageRating = ratingService.getAverageRating(id);
            Long ratingCount = ratingService.getRatingCount(id);
            
            return ResponseEntity.ok(Map.of(
                "rating", savedRating,
                "averageRating", averageRating,
                "ratingCount", ratingCount,
                "message", "Track rated successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid rating", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Toggle favourite status for an audio track.
     * POST /api/audio/{id}/favorite
     * 
     * @param id The track ID
     * @param requestBody JSON body containing userId
     * @return ResponseEntity containing the favourite status
     */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<?> toggleFavourite(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        try {
            // Validate track exists
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
            
            // Extract and validate request parameters
            Object userIdObj = requestBody.get("userId");
            
            if (userIdObj == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing parameter", "message", "userId is required"));
            }
            
            Long userId;
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid parameter", "message", "userId must be a number"));
            }
            
            // Toggle favourite status
            boolean isFavourited = favouriteService.toggleFavourite(userId, id);
            
            // Get updated statistics
            Long favouriteCount = favouriteService.getFavouriteCount(id);
            
            return ResponseEntity.ok(Map.of(
                "isFavourited", isFavourited,
                "favouriteCount", favouriteCount,
                "message", isFavourited ? "Track added to favourites" : "Track removed from favourites"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get rating statistics for a track.
     * GET /api/audio/{id}/ratings
     * 
     * @param id The track ID
     * @return ResponseEntity containing rating statistics
     */
    @GetMapping("/{id}/ratings")
    public ResponseEntity<?> getTrackRatings(@PathVariable Long id) {
        try {
            // Validate track exists
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
            
            Double averageRating = ratingService.getAverageRating(id);
            Long ratingCount = ratingService.getRatingCount(id);
            
            return ResponseEntity.ok(Map.of(
                "trackId", id,
                "averageRating", averageRating,
                "ratingCount", ratingCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get favourite statistics for a track.
     * GET /api/audio/{id}/favourites
     * 
     * @param id The track ID
     * @return ResponseEntity containing favourite statistics
     */
    @GetMapping("/{id}/favourites")
    public ResponseEntity<?> getTrackFavourites(@PathVariable Long id) {
        try {
            // Validate track exists
            Optional<AudioTrack> trackOpt = audioService.getTrackById(id);
            if (trackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Track not found", "message", "Audio track with ID " + id + " not found"));
            }
            
            Long favouriteCount = favouriteService.getFavouriteCount(id);
            
            return ResponseEntity.ok(Map.of(
                "trackId", id,
                "favouriteCount", favouriteCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get user's favorite tracks.
     * GET /api/audio/favorites
     * 
     * @param userId The user ID
     * @return ResponseEntity containing the list of user's favorite tracks
     */
    @GetMapping("/favorites")
    public ResponseEntity<?> getUserFavoriteTracks(@RequestParam("userId") Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing parameter", "message", "userId is required"));
            }
            
            // Get user's favorite track IDs
            List<Favourite> userFavourites = favouriteService.getUserFavourites(userId);
            
            if (userFavourites.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No favorite tracks found",
                    "tracks", new ArrayList<>(),
                    "count", 0
                ));
            }
            
            // Get the actual track details for each favorite
            List<AudioTrack> favoriteTracks = new ArrayList<>();
            for (Favourite favourite : userFavourites) {
                Optional<AudioTrack> track = audioService.getTrackById(favourite.getTrackId());
                track.ifPresent(favoriteTracks::add);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Favorite tracks retrieved successfully",
                "tracks", favoriteTracks,
                "count", favoriteTracks.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Search external music using Deezer API.
     * GET /api/audio/search/external?query={query}
     * 
     * @param query The search query
     * @return ResponseEntity containing external music search results
     */
    @GetMapping("/search/external")
    public ResponseEntity<?> searchExternalMusic(@RequestParam("query") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid query", "message", "Search query cannot be empty"));
            }
            
            List<ExternalMusicService.MusicMetadata> results = externalMusicService.searchMusic(query.trim());
            
            return ResponseEntity.ok(Map.of(
                "message", "Search completed successfully",
                "query", query.trim(),
                "count", results.size(),
                "results", results
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Search failed", "message", e.getMessage()));
        }
    }
    
    /**
     * Get external music search cache statistics.
     * GET /api/audio/search/external/cache/stats
     * 
     * @return ResponseEntity containing cache statistics
     */
    @GetMapping("/search/external/cache/stats")
    public ResponseEntity<?> getExternalSearchCacheStats() {
        try {
            Map<String, Object> stats = externalMusicService.getCacheStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get cache stats", "message", e.getMessage()));
        }
    }
    
    /**
     * Clear external music search cache.
     * DELETE /api/audio/search/external/cache
     * 
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/search/external/cache")
    public ResponseEntity<?> clearExternalSearchCache() {
        try {
            externalMusicService.clearCache();
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to clear cache", "message", e.getMessage()));
        }
    }
}
