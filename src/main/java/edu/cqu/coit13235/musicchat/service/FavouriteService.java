package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.Favourite;
import edu.cqu.coit13235.musicchat.repository.FavouriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing user favourites.
 * Provides business logic for favourite operations and toggle functionality.
 */
@Service
@Transactional
public class FavouriteService {
    
    private static final Logger logger = LoggerFactory.getLogger(FavouriteService.class);
    
    private final FavouriteRepository favouriteRepository;
    
    @Autowired
    public FavouriteService(FavouriteRepository favouriteRepository) {
        this.favouriteRepository = favouriteRepository;
    }
    
    /**
     * Toggle favourite status for a track.
     * If the track is already favourited, it will be removed from favourites.
     * If not favourited, it will be added to favourites.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return true if track is now favourited, false if removed from favourites
     */
    public boolean toggleFavourite(Long userId, Long trackId) {
        logger.debug("Toggling favourite for track {} by user {}", trackId, userId);
        
        Optional<Favourite> existingFavourite = favouriteRepository.findByUserIdAndTrackId(userId, trackId);
        
        if (existingFavourite.isPresent()) {
            // Remove from favourites
            favouriteRepository.delete(existingFavourite.get());
            logger.info("Removed track {} from favourites for user {}", trackId, userId);
            return false;
        } else {
            // Add to favourites
            Favourite favourite = new Favourite(userId, trackId);
            favouriteRepository.save(favourite);
            logger.info("Added track {} to favourites for user {}", trackId, userId);
            return true;
        }
    }
    
    /**
     * Check if a track is favourited by a user.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return true if favourited, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isFavourited(Long userId, Long trackId) {
        return favouriteRepository.existsByUserIdAndTrackId(userId, trackId);
    }
    
    /**
     * Get a user's favourite for a specific track.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return Optional containing the favourite if found
     */
    @Transactional(readOnly = true)
    public Optional<Favourite> getUserFavourite(Long userId, Long trackId) {
        return favouriteRepository.findByUserIdAndTrackId(userId, trackId);
    }
    
    /**
     * Get all favourites for a specific track.
     * 
     * @param trackId the track ID
     * @return List of favourites for the track
     */
    @Transactional(readOnly = true)
    public List<Favourite> getTrackFavourites(Long trackId) {
        return favouriteRepository.findByTrackId(trackId);
    }
    
    /**
     * Get all favourites by a specific user.
     * 
     * @param userId the user ID
     * @return List of favourites by the user
     */
    @Transactional(readOnly = true)
    public List<Favourite> getUserFavourites(Long userId) {
        return favouriteRepository.findByUserId(userId);
    }
    
    /**
     * Get the number of favourites for a track.
     * 
     * @param trackId the track ID
     * @return number of favourites
     */
    @Transactional(readOnly = true)
    public Long getFavouriteCount(Long trackId) {
        return favouriteRepository.countByTrackId(trackId);
    }
    
    /**
     * Add a track to favourites (without toggle).
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return the created favourite
     */
    public Favourite addFavourite(Long userId, Long trackId) {
        if (favouriteRepository.existsByUserIdAndTrackId(userId, trackId)) {
            throw new IllegalStateException("Track is already in favourites");
        }
        
        Favourite favourite = new Favourite(userId, trackId);
        Favourite savedFavourite = favouriteRepository.save(favourite);
        logger.info("Added track {} to favourites for user {}", trackId, userId);
        return savedFavourite;
    }
    
    /**
     * Remove a track from favourites.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return true if favourite was removed, false if no favourite existed
     */
    public boolean removeFavourite(Long userId, Long trackId) {
        Optional<Favourite> favourite = favouriteRepository.findByUserIdAndTrackId(userId, trackId);
        if (favourite.isPresent()) {
            favouriteRepository.delete(favourite.get());
            logger.info("Removed track {} from favourites for user {}", trackId, userId);
            return true;
        }
        return false;
    }
}
