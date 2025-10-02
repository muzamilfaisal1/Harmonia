package edu.cqu.coit13235.musicchat.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for integrating with external music APIs (Deezer).
 * Provides metadata search functionality with caching.
 */
@Service
public class ExternalMusicService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalMusicService.class);
    
    private static final String DEEZER_API_BASE_URL = "https://api.deezer.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    
    private final WebClient webClient;
    
    // Simple in-memory cache for API responses
    private final Map<String, DeezerSearchResponse> cache = new ConcurrentHashMap<>();
    
    @Autowired
    public ExternalMusicService() {
        this.webClient = WebClient.builder()
            .baseUrl(DEEZER_API_BASE_URL)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
    }
    
    /**
     * Search for music metadata using Deezer API.
     * Results are cached to reduce API calls.
     * 
     * @param query the search query
     * @return List of music metadata
     */
    @Cacheable(value = "deezerSearch", key = "#query")
    public List<MusicMetadata> searchMusic(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
        
        String normalizedQuery = query.trim().toLowerCase();
        
        // Check in-memory cache first
        if (cache.containsKey(normalizedQuery)) {
            logger.debug("Returning cached result for query: {}", query);
            DeezerSearchResponse cachedResponse = cache.get(normalizedQuery);
            return cachedResponse.getData().stream()
                .map(DeezerTrack::toMusicMetadata)
                .toList();
        }
        
        logger.info("Searching Deezer API for: {}", query);
        
        try {
            DeezerSearchResponse response = webClient.get()
                .uri("/search?q={query}", query)
                .retrieve()
                .bodyToMono(DeezerSearchResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();
            
            if (response != null && response.getData() != null) {
                // Convert DeezerTrack to MusicMetadata
                List<MusicMetadata> metadata = response.getData().stream()
                    .map(DeezerTrack::toMusicMetadata)
                    .toList();
                
                // Cache the response
                cache.put(normalizedQuery, response);
                logger.info("Found {} results for query: {}", metadata.size(), query);
                return metadata;
            } else {
                logger.warn("No results found for query: {}", query);
                return List.of();
            }
            
        } catch (WebClientResponseException e) {
            logger.error("Deezer API error for query '{}': {} - {}", query, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search music metadata: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error searching Deezer API for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Failed to search music metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "cacheSize", cache.size(),
            "cachedQueries", cache.keySet()
        );
    }
    
    /**
     * Clear the cache.
     */
    public void clearCache() {
        cache.clear();
        logger.info("External music cache cleared");
    }
    
    /**
     * Music metadata DTO for external API responses.
     */
    public static class MusicMetadata {
        private String title;
        private String artist;
        private String album;
        private String coverUrl;
        private String previewUrl;
        private Integer duration;
        
        // Default constructor
        public MusicMetadata() {}
        
        public MusicMetadata(String title, String artist, String album, String coverUrl) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.coverUrl = coverUrl;
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        
        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }
        
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        
        @Override
        public String toString() {
            return "MusicMetadata{" +
                    "title='" + title + '\'' +
                    ", artist='" + artist + '\'' +
                    ", album='" + album + '\'' +
                    ", coverUrl='" + coverUrl + '\'' +
                    ", previewUrl='" + previewUrl + '\'' +
                    ", duration=" + duration +
                    '}';
        }
    }
    
    /**
     * Deezer API response wrapper.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerSearchResponse {
        @JsonProperty("data")
        private List<DeezerTrack> data;
        
        @JsonProperty("total")
        private Integer total;
        
        @JsonProperty("next")
        private String next;
        
        // Getters and setters
        public List<DeezerTrack> getData() { return data; }
        public void setData(List<DeezerTrack> data) { this.data = data; }
        
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
    }
    
    /**
     * Deezer track DTO.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerTrack {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("artist")
        private DeezerArtist artist;
        
        @JsonProperty("album")
        private DeezerAlbum album;
        
        @JsonProperty("preview")
        private String preview;
        
        @JsonProperty("duration")
        private Integer duration;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public DeezerArtist getArtist() { return artist; }
        public void setArtist(DeezerArtist artist) { this.artist = artist; }
        
        public DeezerAlbum getAlbum() { return album; }
        public void setAlbum(DeezerAlbum album) { this.album = album; }
        
        public String getPreview() { return preview; }
        public void setPreview(String preview) { this.preview = preview; }
        
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        
        /**
         * Convert to MusicMetadata.
         */
        public MusicMetadata toMusicMetadata() {
            MusicMetadata metadata = new MusicMetadata();
            metadata.setTitle(this.title);
            metadata.setArtist(this.artist != null ? this.artist.getName() : "Unknown Artist");
            metadata.setAlbum(this.album != null ? this.album.getTitle() : "Unknown Album");
            metadata.setCoverUrl(this.album != null ? this.album.getCover() : null);
            metadata.setPreviewUrl(this.preview);
            metadata.setDuration(this.duration);
            return metadata;
        }
    }
    
    /**
     * Deezer artist DTO.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerArtist {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("name")
        private String name;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * Deezer album DTO.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerAlbum {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("cover")
        private String cover;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getCover() { return cover; }
        public void setCover(String cover) { this.cover = cover; }
    }
}
