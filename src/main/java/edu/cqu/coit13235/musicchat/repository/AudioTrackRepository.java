package edu.cqu.coit13235.musicchat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;

/**
 * Repository interface for AudioTrack entity.
 * Provides data access methods for audio tracks.
 */
@Repository
public interface AudioTrackRepository extends JpaRepository<AudioTrack, Long> {
    
    /**
     * Find audio track by filename.
     * @param filename The filename to search for
     * @return Optional containing the audio track if found
     */
    Optional<AudioTrack> findByFilename(String filename);
    
    /**
     * Find audio tracks by artist.
     * @param artist The artist name to search for
     * @return List of audio tracks by the specified artist
     */
    List<AudioTrack> findByArtistOrderByUploadedAtDesc(String artist);
    
    /**
     * Find audio tracks by artist containing the given text (case-insensitive).
     * @param artist The artist text to search for
     * @return List of audio tracks with artists containing the text
     */
    @Query("SELECT at FROM AudioTrack at WHERE LOWER(at.artist) LIKE LOWER(CONCAT('%', :artist, '%')) ORDER BY at.uploadedAt DESC")
    List<AudioTrack> findByArtistContainingIgnoreCase(String artist);
    
    /**
     * Find audio tracks by title containing the given text (case-insensitive).
     * @param title The title text to search for
     * @return List of audio tracks with titles containing the text
     */
    @Query("SELECT at FROM AudioTrack at WHERE LOWER(at.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY at.uploadedAt DESC")
    List<AudioTrack> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Find all audio tracks ordered by upload date (newest first).
     * @return List of all audio tracks ordered by upload date
     */
    @Query("SELECT at FROM AudioTrack at ORDER BY at.uploadedAt DESC")
    List<AudioTrack> findAllOrderByUploadedAtDesc();
    
    /**
     * Find audio tracks by content type.
     * @param contentType The content type to search for
     * @return List of audio tracks with the specified content type
     */
    List<AudioTrack> findByContentTypeOrderByUploadedAtDesc(String contentType);
    
    /**
     * Count total number of audio tracks.
     * @return Total count of audio tracks
     */
    @Override
    long count();
    
    /**
     * Find audio tracks uploaded by a specific user.
     * @param user The user who uploaded the tracks
     * @return List of audio tracks uploaded by the specified user
     */
    List<AudioTrack> findByUserOrderByUploadedAtDesc(User user);
    
    /**
     * Find audio tracks uploaded by a specific user ID.
     * @param userId The ID of the user who uploaded the tracks
     * @return List of audio tracks uploaded by the specified user
     */
    @Query("SELECT at FROM AudioTrack at WHERE at.user.id = :userId ORDER BY at.uploadedAt DESC")
    List<AudioTrack> findByUserIdOrderByUploadedAtDesc(Long userId);
    
    /**
     * Find audio tracks uploaded on a specific date.
     * @param uploadedAt The upload date range
     * @return List of audio tracks uploaded on the specified date
     */
    @Query("SELECT at FROM AudioTrack at WHERE DATE(at.uploadedAt) = DATE(:uploadedAt) ORDER BY at.uploadedAt DESC")
    List<AudioTrack> findByUploadedAtDate(java.time.LocalDateTime uploadedAt);
}
