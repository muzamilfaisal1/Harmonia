package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.Rating;
import edu.cqu.coit13235.musicchat.repository.RatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing audio track ratings.
 * Provides business logic for rating operations and statistics.
 */
@Service
@Transactional
public class RatingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    
    private final RatingRepository ratingRepository;
    
    @Autowired
    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }
    
    /**
     * Rate a track or update existing rating.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @param ratingValue the rating value (1-5)
     * @return the saved rating
     * @throws IllegalArgumentException if rating value is invalid
     */
    public Rating rateTrack(Long userId, Long trackId, Integer ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating value must be between 1 and 5");
        }
        
        logger.debug("Rating track {} by user {} with value {}", trackId, userId, ratingValue);
        
        // Check if user has already rated this track
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndTrackId(userId, trackId);
        
        if (existingRating.isPresent()) {
            // Update existing rating
            Rating rating = existingRating.get();
            rating.setRatingValue(ratingValue);
            Rating savedRating = ratingRepository.save(rating);
            logger.info("Updated rating for track {} by user {} to {}", trackId, userId, ratingValue);
            return savedRating;
        } else {
            // Create new rating
            Rating rating = new Rating(userId, trackId, ratingValue);
            Rating savedRating = ratingRepository.save(rating);
            logger.info("Created new rating for track {} by user {} with value {}", trackId, userId, ratingValue);
            return savedRating;
        }
    }
    
    /**
     * Get a user's rating for a specific track.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return Optional containing the rating if found
     */
    @Transactional(readOnly = true)
    public Optional<Rating> getUserRating(Long userId, Long trackId) {
        return ratingRepository.findByUserIdAndTrackId(userId, trackId);
    }
    
    /**
     * Get all ratings for a specific track.
     * 
     * @param trackId the track ID
     * @return List of ratings for the track
     */
    @Transactional(readOnly = true)
    public List<Rating> getTrackRatings(Long trackId) {
        return ratingRepository.findByTrackId(trackId);
    }
    
    /**
     * Get all ratings by a specific user.
     * 
     * @param userId the user ID
     * @return List of ratings by the user
     */
    @Transactional(readOnly = true)
    public List<Rating> getUserRatings(Long userId) {
        return ratingRepository.findByUserId(userId);
    }
    
    /**
     * Get the average rating for a track.
     * 
     * @param trackId the track ID
     * @return average rating value, or null if no ratings exist
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long trackId) {
        Double average = ratingRepository.findAverageRatingByTrackId(trackId);
        return average != null ? Math.round(average * 100.0) / 100.0 : null; // Round to 2 decimal places
    }
    
    /**
     * Get the number of ratings for a track.
     * 
     * @param trackId the track ID
     * @return number of ratings
     */
    @Transactional(readOnly = true)
    public Long getRatingCount(Long trackId) {
        return ratingRepository.countByTrackId(trackId);
    }
    
    /**
     * Remove a user's rating for a track.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return true if rating was removed, false if no rating existed
     */
    public boolean removeRating(Long userId, Long trackId) {
        Optional<Rating> rating = ratingRepository.findByUserIdAndTrackId(userId, trackId);
        if (rating.isPresent()) {
            ratingRepository.delete(rating.get());
            logger.info("Removed rating for track {} by user {}", trackId, userId);
            return true;
        }
        return false;
    }
}
