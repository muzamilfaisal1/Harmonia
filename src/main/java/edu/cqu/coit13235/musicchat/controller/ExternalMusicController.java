package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.service.ExternalMusicService;
import edu.cqu.coit13235.musicchat.service.ExternalMusicService.MusicMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for external music metadata operations.
 * Provides endpoints for searching music metadata from external APIs.
 */
@RestController
@RequestMapping("/api/external")
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class ExternalMusicController {
    
    private final ExternalMusicService externalMusicService;
    
    @Autowired
    public ExternalMusicController(ExternalMusicService externalMusicService) {
        this.externalMusicService = externalMusicService;
    }
    
    /**
     * Search for music metadata using external APIs.
     * GET /api/external/search?query={query}
     * 
     * @param query the search query
     * @return ResponseEntity containing the list of music metadata
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMusic(@RequestParam("query") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid query", "message", "Search query cannot be null or empty"));
            }
            
            List<MusicMetadata> results = externalMusicService.searchMusic(query.trim());
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", results,
                "count", results.size(),
                "message", "Search completed successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid input", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Search failed", "message", e.getMessage()));
        }
    }
    
    /**
     * Get cache statistics for external music service.
     * GET /api/external/cache/stats
     * 
     * @return ResponseEntity containing cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<?> getCacheStats() {
        try {
            Map<String, Object> stats = externalMusicService.getCacheStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get cache stats", "message", e.getMessage()));
        }
    }
    
    /**
     * Clear the external music cache.
     * DELETE /api/external/cache
     * 
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        try {
            externalMusicService.clearCache();
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to clear cache", "message", e.getMessage()));
        }
    }
}
