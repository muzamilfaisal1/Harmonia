package edu.cqu.coit13235.musicchat.repository;

import edu.cqu.coit13235.musicchat.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Rating entity operations.
 * Provides methods for rating data access and statistics.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    /**
     * Find a rating by user ID and track ID.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return Optional containing the rating if found, empty otherwise
     */
    Optional<Rating> findByUserIdAndTrackId(Long userId, Long trackId);
    
    /**
     * Find all ratings for a specific track.
     * 
     * @param trackId the track ID
     * @return List of ratings for the track
     */
    List<Rating> findByTrackId(Long trackId);
    
    /**
     * Find all ratings by a specific user.
     * 
     * @param userId the user ID
     * @return List of ratings by the user
     */
    List<Rating> findByUserId(Long userId);
    
    /**
     * Calculate the average rating for a track.
     * 
     * @param trackId the track ID
     * @return average rating value
     */
    @Query("SELECT AVG(r.ratingValue) FROM Rating r WHERE r.trackId = :trackId")
    Double findAverageRatingByTrackId(@Param("trackId") Long trackId);
    
    /**
     * Count the number of ratings for a track.
     * 
     * @param trackId the track ID
     * @return number of ratings
     */
    Long countByTrackId(Long trackId);
}
